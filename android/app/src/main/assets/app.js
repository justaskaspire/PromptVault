const STORAGE_KEY = "promptvault.prompts.v1";
const CATEGORIES = ["AI Instructions","Writing","Coding","Research","Learning","Business","Image Generation","Other"];
const $ = (id) => document.getElementById(id);
let prompts = loadPrompts();
let activeFilter = "all";
let toastTimer;

function loadPrompts(){
  try { const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]"); return Array.isArray(saved) ? saved : []; }
  catch { return []; }
}
function savePrompts(){ localStorage.setItem(STORAGE_KEY, JSON.stringify(prompts)); }
function escapeHTML(value=""){ return String(value).replace(/[&<>"']/g, c => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c])); }
function showToast(message){
  const toast=$("toast"); toast.textContent=message; toast.classList.add("show");
  clearTimeout(toastTimer); toastTimer=setTimeout(()=>toast.classList.remove("show"),2200);
}
function niceDate(value){
  if(!value) return "Just now";
  const date=new Date(value); if(Number.isNaN(date.getTime())) return "Just now";
  return date.toLocaleDateString(undefined,{month:"short",day:"numeric"});
}
function renderCategories(){
  $("categoryList").innerHTML = CATEGORIES.map(cat => {
    const count=prompts.filter(p=>p.category===cat).length;
    return `<button class="nav-item ${activeFilter===cat?"active":""}" data-filter="${escapeHTML(cat)}"><span>◇</span>${escapeHTML(cat)}<span class="nav-count">${count}</span></button>`;
  }).join("");
}
function render(){
  renderCategories();
  const favorites=prompts.filter(p=>p.favorite).length;
  $("allCount").textContent=prompts.length;
  $("statTotal").textContent=prompts.length;
  $("statFavorites").textContent=favorites;
  $("statCategories").textContent=new Set(prompts.map(p=>p.category)).size;
  const names={all:"All prompts",favorites:"Favorites"};
  const title=names[activeFilter]||activeFilter;
  $("pageTitle").textContent=title; $("currentView").textContent=title;
  $("listHeading").textContent=activeFilter==="favorites"?"Your favorites":activeFilter==="all"?"Your prompts":activeFilter;
  const query=$("searchInput").value.trim().toLowerCase();
  let shown=prompts.filter(p=>{
    const matchesFilter=activeFilter==="all" || (activeFilter==="favorites"?p.favorite:p.category===activeFilter);
    const haystack=[p.title,p.content,p.category,...(p.tags||[])].join(" ").toLowerCase();
    return matchesFilter && haystack.includes(query);
  });
  const sort=$("sortSelect").value;
  shown.sort((a,b)=>sort==="az"?a.title.localeCompare(b.title):sort==="created"?new Date(b.createdAt)-new Date(a.createdAt):new Date(b.updatedAt)-new Date(a.updatedAt));
  $("resultCount").textContent=`${shown.length} ${shown.length===1?"item":"items"}`;
  if(!shown.length){
    const hasAny=prompts.length>0;
    $("promptGrid").innerHTML=`<div class="empty-state"><div class="empty-icon">✧</div><h3>${query?"No matching prompts":hasAny?"Nothing here yet":"Your prompt library starts here"}</h3><p>${query?"Try another search term.":hasAny?"Save a prompt in this category or choose another view.":"Add your first prompt and keep your best AI instructions in one place."}</p>${!query&&(!hasAny||activeFilter!=="all")?'<button class="primary-button" id="emptyAddBtn">＋ Add a prompt</button>':""}</div>`;
    const btn=$("emptyAddBtn"); if(btn) btn.addEventListener("click",()=>openModal());
    return;
  }
  $("promptGrid").innerHTML=shown.map(p=>`
    <article class="prompt-card">
      <div class="card-top"><span class="category-pill">${escapeHTML(p.category)}</span><div class="card-actions">
        <button class="mini-action ${p.favorite?"favorite":""}" data-action="favorite" data-id="${p.id}" title="Toggle favorite" aria-label="Toggle favorite">${p.favorite?"★":"☆"}</button>
        <button class="mini-action" data-action="edit" data-id="${p.id}" title="Edit" aria-label="Edit prompt">✎</button>
        <button class="mini-action" data-action="delete" data-id="${p.id}" title="Delete" aria-label="Delete prompt">⌫</button>
      </div></div>
      <h3>${escapeHTML(p.title)}</h3><p class="prompt-preview">${escapeHTML(p.content)}</p>
      <div class="tag-row">${(p.tags||[]).slice(0,4).map(t=>`<span class="tag">#${escapeHTML(t)}</span>`).join("")}</div>
      <div class="card-bottom"><span class="updated-label">Updated ${niceDate(p.updatedAt)}</span><button class="copy-button" data-action="copy" data-id="${p.id}">▣ Copy prompt</button></div>
    </article>`).join("");
}
function openModal(prompt=null){
  $("promptForm").reset();
  $("promptId").value=prompt?.id||"";
  $("promptTitle").value=prompt?.title||"";
  $("promptCategory").value=prompt?.category||"AI Instructions";
  $("promptTags").value=(prompt?.tags||[]).join(", ");
  $("promptContent").value=prompt?.content||"";
  $("modalTitle").textContent=prompt?"Edit prompt":"Create a prompt";
  $("modalBackdrop").hidden=false;
  setTimeout(()=>$("promptTitle").focus(),50);
}
function closeModal(){ $("modalBackdrop").hidden=true; }
function addPrompt(data){
  const now=new Date().toISOString();
  prompts.unshift({id:crypto.randomUUID?crypto.randomUUID():String(Date.now()),...data,favorite:false,createdAt:now,updatedAt:now});
  savePrompts(); render(); showToast("Prompt saved.");
}
function handleForm(event){
  event.preventDefault();
  const id=$("promptId").value;
  const data={title:$("promptTitle").value.trim(),category:$("promptCategory").value,content:$("promptContent").value.trim(),tags:$("promptTags").value.split(",").map(t=>t.trim()).filter(Boolean).slice(0,10)};
  if(!data.title||!data.content){showToast("Add a name and prompt text first.");return;}
  if(id){
    const p=prompts.find(item=>item.id===id);
    if(p){Object.assign(p,data,{updatedAt:new Date().toISOString()});}
    showToast("Prompt updated.");
  } else addPrompt(data);
  savePrompts(); render(); closeModal();
}
async function copyPrompt(id){
  const p=prompts.find(item=>item.id===id); if(!p)return;
  if(window.AndroidBridge && typeof AndroidBridge.copyText==="function"){AndroidBridge.copyText(p.content);showToast("Copied to clipboard!");return;}
  try{
    await navigator.clipboard.writeText(p.content);
    showToast("Copied to clipboard!");
  }catch{
    const area=document.createElement("textarea"); area.value=p.content; document.body.appendChild(area); area.select();
    const ok=document.execCommand("copy"); area.remove(); showToast(ok?"Copied to clipboard!":"Copy failed — select and copy manually.");
  }
}
function handleCardAction(event){
  const btn=event.target.closest("[data-action]"); if(!btn)return;
  const {action,id}=btn.dataset; const p=prompts.find(item=>item.id===id); if(!p)return;
  if(action==="copy") copyPrompt(id);
  if(action==="edit") openModal(p);
  if(action==="favorite"){p.favorite=!p.favorite;p.updatedAt=new Date().toISOString();savePrompts();render();showToast(p.favorite?"Added to favorites.":"Removed from favorites.");}
  if(action==="delete" && confirm(`Delete "${p.title}"? This cannot be undone.`)){prompts=prompts.filter(item=>item.id!==id);savePrompts();render();showToast("Prompt deleted.");}
}
function exportBackup(){
  const backup=JSON.stringify({app:"PromptVault",version:1,exportedAt:new Date().toISOString(),prompts},null,2);
  if(window.AndroidBridge && typeof AndroidBridge.saveBackup==="function"){AndroidBridge.saveBackup(backup);return;}
  const blob=new Blob([backup],{type:"application/json"});
  const url=URL.createObjectURL(blob);const a=document.createElement("a");a.href=url;a.download="promptvault-backup.json";a.click();URL.revokeObjectURL(url);showToast("Backup exported.");
}
function importBackup(file){
  const reader=new FileReader();
  reader.onload=()=>{
    try{
      const data=JSON.parse(reader.result);const incoming=Array.isArray(data)?data:data.prompts;
      if(!Array.isArray(incoming))throw new Error("Invalid backup");
      const valid=incoming.filter(p=>p&&typeof p.title==="string"&&typeof p.content==="string").map(p=>({...p,id:String(p.id||Date.now()+Math.random()),category:CATEGORIES.includes(p.category)?p.category:"Other",tags:Array.isArray(p.tags)?p.tags:[],favorite:Boolean(p.favorite),createdAt:p.createdAt||new Date().toISOString(),updatedAt:p.updatedAt||new Date().toISOString()}));
      if(!valid.length)throw new Error("No valid prompts");
      if(confirm(`Import ${valid.length} prompts? This will add them to your current library.`)){prompts=[...valid,...prompts];savePrompts();render();showToast("Backup imported.");}
    }catch{showToast("That file is not a valid PromptVault backup.");}
    $("importFile").value="";
  };
  reader.readAsText(file);
}
$("newPromptBtn").addEventListener("click",()=>openModal());
$("newPromptBtn2").addEventListener("click",()=>openModal());
$("closeModal").addEventListener("click",closeModal);
$("cancelModal").addEventListener("click",closeModal);
$("modalBackdrop").addEventListener("click",e=>{if(e.target===$("modalBackdrop"))closeModal();});
$("promptForm").addEventListener("submit",handleForm);
$("promptGrid").addEventListener("click",handleCardAction);
$("searchInput").addEventListener("input",render);
$("sortSelect").addEventListener("change",render);
document.addEventListener("click",e=>{
  const nav=e.target.closest("[data-filter]");if(nav){activeFilter=nav.dataset.filter;document.querySelectorAll(".nav-item").forEach(n=>n.classList.toggle("active",n.dataset.filter===activeFilter));render();if(innerWidth<=700)$("sidebar").classList.remove("open");}
});
$("exportBtn").addEventListener("click",exportBackup);
$("importBtn").addEventListener("click",()=>$("importFile").click());
$("importFile").addEventListener("change",e=>{if(e.target.files[0])importBackup(e.target.files[0]);});
$("menuBtn").addEventListener("click",()=>$("sidebar").classList.toggle("open"));

// Close the open mobile sidebar with a left-swipe gesture.
(function enableSidebarSwipeClose(){
  const sidebar=$("sidebar");
  let startX=0, startY=0, tracking=false;
  sidebar.addEventListener("touchstart",event=>{
    if(!sidebar.classList.contains("open") || !event.touches.length)return;
    startX=event.touches[0].clientX;
    startY=event.touches[0].clientY;
    tracking=true;
  },{passive:true});
  sidebar.addEventListener("touchend",event=>{
    if(!tracking || !event.changedTouches.length)return;
    const dx=event.changedTouches[0].clientX-startX;
    const dy=event.changedTouches[0].clientY-startY;
    if(dx< -65 && Math.abs(dx)>Math.abs(dy)*1.25) sidebar.classList.remove("open");
    tracking=false;
  },{passive:true});
  sidebar.addEventListener("touchcancel",()=>{tracking=false;},{passive:true});
})();
function setTheme(theme){
  document.documentElement.dataset.theme=theme;
  if(window.AndroidBridge && typeof AndroidBridge.setTheme==="function") AndroidBridge.setTheme(theme);
  localStorage.setItem("promptvault.theme",theme);
  const button=$("themeBtn");
  if(button){button.innerHTML=theme==="dark"?"☀ <span>Light mode</span>":"☾ <span>Dark mode</span>";button.setAttribute("aria-label",theme==="dark"?"Switch to light mode":"Switch to dark mode");}
}
const savedTheme=localStorage.getItem("promptvault.theme");
setTheme(savedTheme==="dark"?"dark":"light");
$("themeBtn").addEventListener("click",()=>setTheme(document.documentElement.dataset.theme==="dark"?"light":"dark"));
document.addEventListener("keydown",e=>{if(e.key==="Escape")closeModal();if((e.ctrlKey||e.metaKey)&&e.key.toLowerCase()==="k"){e.preventDefault();$("searchInput").focus();}});
if("serviceWorker" in navigator && location.protocol.startsWith("http"))window.addEventListener("load",()=>navigator.serviceWorker.register("./sw.js").catch(()=>{}));
render();