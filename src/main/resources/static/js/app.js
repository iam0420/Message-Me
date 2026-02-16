// ======== STATE ========
const API='';let currentUser=null,currentToken=null,currentChat=null,currentChatType=null;
let stompClient=null,isConnected=false,reconnectAttempts=0,allUsers=[],conversations=[],groups=[],typingTimeout=null,selectedFile=null;

// ======== UTILS ========
function getHeaders(){return{'Content-Type':'application/json','Authorization':'Bearer '+currentToken}}
async function apiCall(method,url,body=null){
    const opts={method,headers:getHeaders()};if(body)opts.body=JSON.stringify(body);
    try{const r=await fetch(API+url,opts);const d=await r.json();if(!r.ok)throw new Error(d.message||'Failed');return d}
    catch(e){console.error('API:',e);showToast(e.message,'error');throw e}}
function showToast(msg,type='info'){const t=document.getElementById('toast');document.getElementById('toast-message').textContent=msg;t.className='toast '+type;setTimeout(()=>t.classList.add('hidden'),3000)}
function formatTime(d){if(!d)return'';const dt=new Date(d),now=new Date(),diff=Math.floor((now-dt)/864e5);const t=dt.toLocaleTimeString([],{hour:'2-digit',minute:'2-digit'});if(diff===0)return t;if(diff===1)return'Yesterday';if(diff<7)return dt.toLocaleDateString([],{weekday:'short'});return dt.toLocaleDateString([],{month:'short',day:'numeric'})}
function formatFullTime(d){return d?new Date(d).toLocaleTimeString([],{hour:'2-digit',minute:'2-digit'}):''}
function formatDate(d){if(!d)return'';const dt=new Date(d),now=new Date(),diff=Math.floor((now-dt)/864e5);if(diff===0)return'Today';if(diff===1)return'Yesterday';return dt.toLocaleDateString([],{weekday:'long',month:'long',day:'numeric'})}
function formatFileSize(b){if(!b)return'';if(b<1024)return b+' B';if(b<1048576)return(b/1024).toFixed(1)+' KB';return(b/1048576).toFixed(1)+' MB'}
function esc(t){const d=document.createElement('div');d.textContent=t;return d.innerHTML}
function save(k,v){localStorage.setItem(k,JSON.stringify(v))}
function load(k){const i=localStorage.getItem(k);return i?JSON.parse(i):null}

// ======== AUTH ========
function showLogin(){document.getElementById('login-form').classList.add('active');document.getElementById('signup-form').classList.remove('active')}
function showSignup(){document.getElementById('login-form').classList.remove('active');document.getElementById('signup-form').classList.add('active')}
async function login(){
    const u=document.getElementById('login-username').value.trim(),p=document.getElementById('login-password').value.trim();
    if(!u||!p){showToast('Fill all fields','error');return}
    try{const r=await fetch(API+'/api/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username:u,password:p})});
    const d=await r.json();if(!r.ok)throw new Error(d.message||'Login failed');handleAuth(d.data);showToast('Login successful!','success')}catch(e){showToast(e.message,'error')}}
async function signup(){
    const u=document.getElementById('signup-username').value.trim(),e=document.getElementById('signup-email').value.trim(),
    dn=document.getElementById('signup-displayname').value.trim(),p=document.getElementById('signup-password').value.trim();
    if(!u||!e||!p){showToast('Fill required fields','error');return}
    try{const r=await fetch(API+'/api/auth/signup',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username:u,email:e,password:p,displayName:dn})});
    const d=await r.json();if(!r.ok)throw new Error(d.message||'Signup failed');handleAuth(d.data);showToast('Account created!','success')}catch(er){showToast(er.message,'error')}}
function handleAuth(d){currentToken=d.accessToken;currentUser={id:d.userId,username:d.username,displayName:d.displayName||d.username,email:d.email};save('auth',{token:currentToken,user:currentUser});showChatScreen();connectWS();loadData()}
function logout(){disconnectWS();currentToken=null;currentUser=null;currentChat=null;localStorage.removeItem('auth');showAuthScreen();showToast('Logged out','success')}
function checkAuth(){const s=load('auth');if(s&&s.token&&s.user){currentToken=s.token;currentUser=s.user;showChatScreen();connectWS();loadData();return true}return false}
function showAuthScreen(){document.getElementById('auth-screen').classList.add('active');document.getElementById('chat-screen').classList.remove('active')}
function showChatScreen(){document.getElementById('auth-screen').classList.remove('active');document.getElementById('chat-screen').classList.add('active');document.getElementById('current-user-name').textContent=currentUser.displayName}

// ======== WEBSOCKET ========
function connectWS(){
    if(isConnected||!currentToken)return;const sock=new SockJS(API+'/ws');stompClient=Stomp.over(sock);stompClient.debug=()=>{};
    stompClient.connect({'Authorization':'Bearer '+currentToken},()=>{
        isConnected=true;reconnectAttempts=0;console.log('WS Connected');
        stompClient.subscribe('/user/'+currentUser.id+'/queue/messages',m=>handleMsg(JSON.parse(m.body)));
        stompClient.subscribe('/user/'+currentUser.id+'/queue/typing',m=>handleTyping(JSON.parse(m.body)));
        stompClient.subscribe('/user/'+currentUser.id+'/queue/message-status',m=>handleMsgStatus(JSON.parse(m.body)));
        stompClient.subscribe('/topic/online-status',m=>handleOnline(JSON.parse(m.body)));
    },e=>{isConnected=false;console.error('WS Error:',e);if(reconnectAttempts<5){reconnectAttempts++;setTimeout(connectWS,3000*reconnectAttempts)}else showToast('Connection lost','error')})}
function disconnectWS(){if(stompClient&&isConnected){stompClient.disconnect();isConnected=false}}
function wsSend(dest,body){if(!stompClient||!isConnected){showToast('Not connected','error');connectWS();return}stompClient.send(dest,{},JSON.stringify(body))}

// ======== DATA LOADING ========
async function loadData(){await Promise.all([loadUsers(),loadConversations(),loadGroups()])}
async function loadUsers(){try{const r=await apiCall('GET','/api/users');allUsers=r.data||[];renderUsers(allUsers)}catch(e){}}
async function loadConversations(){try{const r=await apiCall('GET','/api/chat/conversations');conversations=r.data||[];renderConvos(conversations)}catch(e){}}
async function loadGroups(){try{const r=await apiCall('GET','/api/groups');groups=r.data||[];renderGroups(groups)}catch(e){}}

// ======== RENDERING ========
function renderUsers(users){
    const c=document.getElementById('user-list');
    if(!users.length){c.innerHTML='<div style="padding:20px;text-align:center;color:var(--text-secondary)">No users found</div>';return}
    c.innerHTML=users.map(u=>'<div class="list-item" onclick="openPrivateChat('+u.id+',\''+esc(u.displayName||u.username)+'\')"><div class="avatar" style="position:relative"><i class="fas fa-user"></i>'+(u.status==='ONLINE'?'<span class="online-dot"></span>':'')+'</div><div class="list-item-info"><div class="list-item-name">'+esc(u.displayName||u.username)+'</div><div class="list-item-last-msg">'+(u.about||'@'+u.username)+'</div></div><span class="status-text'+(u.status==='ONLINE'?' online':'')+'">'+(u.status==='ONLINE'?'online':'offline')+'</span></div>').join('')}
function renderConvos(convos){
    const c=document.getElementById('chat-list');
    if(!convos.length){c.innerHTML='<div style="padding:20px;text-align:center;color:var(--text-secondary)">No conversations yet</div>';return}
    c.innerHTML=convos.map(cv=>{const u=cv.user;return'<div class="list-item" onclick="openPrivateChat('+u.id+',\''+esc(u.displayName||u.username)+'\')"><div class="avatar" style="position:relative"><i class="fas fa-user"></i>'+(u.status==='ONLINE'?'<span class="online-dot"></span>':'')+'</div><div class="list-item-info"><div class="list-item-name">'+esc(u.displayName||u.username)+'</div><div class="list-item-last-msg">Click to chat</div></div>'+(cv.lastMessageAt?'<span class="list-item-time">'+formatTime(cv.lastMessageAt)+'</span>':'')+'</div>'}).join('')}
function renderGroups(gl){
    const c=document.getElementById('group-list');
    if(!gl.length){c.innerHTML='<div style="padding:20px;text-align:center;color:var(--text-secondary)">No groups yet</div>';return}
    c.innerHTML=gl.map(g=>'<div class="list-item" onclick="openGroupChat('+g.id+',\''+esc(g.name)+'\')"><div class="avatar"><i class="fas fa-users"></i></div><div class="list-item-info"><div class="list-item-name">'+esc(g.name)+'</div><div class="list-item-last-msg">'+(g.members?g.members.length+' members':'')+'</div></div>'+(g.lastMessageAt?'<span class="list-item-time">'+formatTime(g.lastMessageAt)+'</span>':'')+'</div>').join('')}

// ======== CHAT ========
async function openPrivateChat(userId,name){
    currentChat={id:userId,name};currentChatType='private';
    document.getElementById('chat-partner-name').textContent=name;
    document.getElementById('chat-header').classList.remove('hidden');
    document.getElementById('messages-container').classList.remove('hidden');
    document.getElementById('message-input-area').classList.remove('hidden');
    document.getElementById('empty-state').style.display='none';
    try{const r=await apiCall('GET','/api/users/'+userId+'/online');const s=document.getElementById('chat-partner-status');s.textContent=r.data?'online':'offline';s.className='status-text'+(r.data?' online':'')}catch(e){}
    try{const r=await apiCall('GET','/api/chat/history/'+userId+'?page=0&size=100');renderMsgs((r.data||[]).reverse())}catch(e){}
    try{await apiCall('POST','/api/chat/read/'+userId)}catch(e){}
    if(window.innerWidth<=768)document.getElementById('sidebar').classList.add('hidden-mobile');
    document.getElementById('message-input').focus();loadConversations()}
async function openGroupChat(groupId,name){
    currentChat={id:groupId,name};currentChatType='group';
    document.getElementById('chat-partner-name').textContent=name;
    document.getElementById('chat-partner-status').textContent='group';
    document.getElementById('chat-header').classList.remove('hidden');
    document.getElementById('messages-container').classList.remove('hidden');
    document.getElementById('message-input-area').classList.remove('hidden');
    document.getElementById('empty-state').style.display='none';
    try{const r=await apiCall('GET','/api/groups/'+groupId+'/messages?page=0&size=100');renderMsgs((r.data||[]).reverse())}catch(e){}
    if(window.innerWidth<=768)document.getElementById('sidebar').classList.add('hidden-mobile');
    document.getElementById('message-input').focus();loadGroups()}

function renderMsgs(msgs){
    const c=document.getElementById('messages');let html='',lastDate='';
    msgs.forEach(m=>{
        const d=formatDate(m.timestamp);if(d!==lastDate){html+='<div class="date-separator"><span>'+d+'</span></div>';lastDate=d}
        const sent=m.senderId===currentUser.id,si=getStatusIcon(m.status,sent);
        let fh='';if(m.fileUrl){if(m.type==='IMAGE')fh='<div class="message-file"><img src="'+m.fileUrl+'" onclick="window.open(\''+m.fileUrl+'\')"></div>';else fh='<div class="message-file"><a href="'+m.fileUrl+'" target="_blank"><i class="fas fa-file"></i> '+esc(m.fileName||'File')+'</a></div>'}
        html+='<div class="message '+(sent?'sent':'received')+'" data-message-id="'+m.id+'">';
        if(!sent&&currentChatType==='group')html+='<div class="message-sender">'+esc(m.senderName)+'</div>';
        html+=fh;if(m.content)html+='<div class="message-content">'+esc(m.content)+'</div>';
        html+='<div class="message-meta"><span class="message-time">'+formatFullTime(m.timestamp)+'</span>'+(sent?'<span class="message-status">'+si+'</span>':'')+'</div></div>'});
    c.innerHTML=html;scrollBottom()}
function getStatusIcon(s,sent){if(!sent)return'';switch(s){case'SENT':return'<i class="fas fa-check"></i>';case'DELIVERED':return'<i class="fas fa-check-double delivered"></i>';case'READ':return'<i class="fas fa-check-double read"></i>';default:return'<i class="fas fa-clock"></i>'}}
function appendMsg(m){
    const c=document.getElementById('messages'),sent=m.senderId===currentUser.id,si=getStatusIcon(m.status,sent);
    let fh='';if(m.fileUrl){if(m.type==='IMAGE')fh='<div class="message-file"><img src="'+m.fileUrl+'" onclick="window.open(\''+m.fileUrl+'\')"></div>';else fh='<div class="message-file"><a href="'+m.fileUrl+'" target="_blank"><i class="fas fa-file"></i> '+esc(m.fileName||'File')+'</a></div>'}
    let h='<div class="message '+(sent?'sent':'received')+'" data-message-id="'+m.id+'">';
    if(!sent&&currentChatType==='group')h+='<div class="message-sender">'+esc(m.senderName)+'</div>';
    h+=fh;if(m.content)h+='<div class="message-content">'+esc(m.content)+'</div>';
    h+='<div class="message-meta"><span class="message-time">'+formatFullTime(m.timestamp)+'</span>'+(sent?'<span class="message-status">'+si+'</span>':'')+'</div></div>';
    c.insertAdjacentHTML('beforeend',h);scrollBottom()}
function scrollBottom(){const c=document.getElementById('messages-container');setTimeout(()=>{c.scrollTop=c.scrollHeight},50)}

function sendMessage(){
    const inp=document.getElementById('message-input'),content=inp.value.trim();
    if(!content&&!selectedFile)return;if(!currentChat)return;
    const msg={content,type:selectedFile?(selectedFile.type.startsWith('image/')?'IMAGE':'DOCUMENT'):'TEXT'};
    if(currentChatType==='group')msg.groupId=currentChat.id;else msg.receiverId=currentChat.id;
    if(selectedFile){uploadAndSend(msg)}else{wsSend('/app/chat.send',msg);inp.value=''}
    sendTypingStatus(false)}
async function uploadAndSend(msg){
    if(!selectedFile)return;
    try{const fd=new FormData();fd.append('file',selectedFile);
    const r=await fetch(API+'/api/files/upload',{method:'POST',headers:{'Authorization':'Bearer '+currentToken},body:fd});
    const d=await r.json();if(d.success){msg.fileUrl=d.data.fileUrl;msg.fileName=d.data.fileName;msg.fileSize=d.data.fileSize;wsSend('/app/chat.send',msg)}}
    catch(e){showToast('Upload failed','error')}
    cancelFileUpload();document.getElementById('message-input').value=''}

// ======== HANDLERS ========
function handleMsg(m){
    if(currentChat){const isCur=(currentChatType==='private'&&(m.senderId===currentChat.id||m.receiverId===currentChat.id))||(currentChatType==='group'&&m.groupId===currentChat.id);
    if(isCur){appendMsg(m);if(m.senderId!==currentUser.id)wsSend('/app/chat.read',{messageId:m.id});return}}
    if(m.senderId!==currentUser.id){showToast('New message from '+m.senderName,'info');loadConversations()}}
function handleTyping(d){if(!currentChat)return;const rel=(currentChatType==='private'&&d.senderId===currentChat.id)||currentChatType==='group';
    if(rel){const i=document.getElementById('typing-indicator');d.typing?i.classList.remove('hidden'):i.classList.add('hidden')}}
function handleMsgStatus(d){const el=document.querySelector('.message[data-message-id="'+d.messageId+'"] .message-status');if(el)el.innerHTML=getStatusIcon(d.status,true)}
function handleOnline(d){
    if(currentChat&&currentChatType==='private'&&currentChat.id===d.userId){const s=document.getElementById('chat-partner-status');s.textContent=d.online?'online':'offline';s.className='status-text'+(d.online?' online':'')}
    allUsers=allUsers.map(u=>{if(u.id===d.userId)u.status=d.online?'ONLINE':'OFFLINE';return u});renderUsers(allUsers)}
function handleTypingInput(){sendTypingStatus(true);if(typingTimeout)clearTimeout(typingTimeout);typingTimeout=setTimeout(()=>sendTypingStatus(false),2000)}
function sendTypingStatus(t){if(!currentChat)return;const d={typing:t};if(currentChatType==='group')d.groupId=currentChat.id;else d.receiverId=currentChat.id;wsSend('/app/chat.typing',d)}

// ======== UI HELPERS ========
function switchTab(name,el){document.querySelectorAll('.tab').forEach(t=>t.classList.remove('active'));document.querySelectorAll('.tab-content').forEach(t=>t.classList.remove('active'));
    if(el)el.classList.add('active');else document.querySelector('.tab').classList.add('active');document.getElementById(name+'-tab').classList.add('active')}
function handleSearch(kw){if(!kw){renderUsers(allUsers);return}const f=allUsers.filter(u=>u.username.toLowerCase().includes(kw.toLowerCase())||(u.displayName&&u.displayName.toLowerCase().includes(kw.toLowerCase())));renderUsers(f);switchTab('users')}
function goBack(){document.getElementById('sidebar').classList.remove('hidden-mobile');currentChat=null;currentChatType=null;
    document.getElementById('chat-header').classList.add('hidden');document.getElementById('messages-container').classList.add('hidden');
    document.getElementById('message-input-area').classList.add('hidden');document.getElementById('empty-state').style.display=''}
function handleFileSelect(e){const f=e.target.files[0];if(!f)return;selectedFile=f;const p=document.getElementById('file-preview');
    if(f.type.startsWith('image/')){const r=new FileReader();r.onload=ev=>{document.getElementById('file-preview-img').src=ev.target.result;document.getElementById('file-preview-img').style.display='block'};r.readAsDataURL(f)}
    else document.getElementById('file-preview-img').style.display='none';
    document.getElementById('file-preview-name').textContent=f.name;document.getElementById('file-preview-size').textContent=formatFileSize(f.size);p.classList.remove('hidden')}
function cancelFileUpload(){selectedFile=null;document.getElementById('file-input').value='';document.getElementById('file-preview').classList.add('hidden')}
function insertEmoji(){const emojis=['😊','😂','❤️','👍','🎉','🔥','😍','🤔','👋','🙏','💪','✨'];const inp=document.getElementById('message-input');inp.value+=emojis[Math.floor(Math.random()*emojis.length)];inp.focus()}
function showNewGroupModal(){const m=document.getElementById('group-modal'),s=document.getElementById('group-members-select');
    s.innerHTML=allUsers.map(u=>'<label class="member-option"><input type="checkbox" value="'+u.id+'"><span>'+esc(u.displayName||u.username)+'</span></label>').join('');m.classList.remove('hidden')}
function hideNewGroupModal(){document.getElementById('group-modal').classList.add('hidden');document.getElementById('group-name').value='';document.getElementById('group-description').value=''}
async function createGroup(){const n=document.getElementById('group-name').value.trim(),d=document.getElementById('group-description').value.trim();
    const cbs=document.querySelectorAll('#group-members-select input:checked'),ids=Array.from(cbs).map(c=>parseInt(c.value));
    if(!n){showToast('Enter group name','error');return}if(!ids.length){showToast('Select members','error');return}
    try{await apiCall('POST','/api/groups',{name:n,description:d,memberIds:ids});showToast('Group created!','success');hideNewGroupModal();loadGroups()}catch(e){}}

// ======== INIT ========
document.addEventListener('DOMContentLoaded',()=>{
    if(!checkAuth())showAuthScreen();
    document.addEventListener('visibilitychange',()=>{if(!document.hidden&&currentToken&&!isConnected)connectWS()});
    window.addEventListener('resize',()=>{if(window.innerWidth>768)document.getElementById('sidebar').classList.remove('hidden-mobile')});
    setInterval(()=>{if(currentToken)loadConversations()},30000)});
