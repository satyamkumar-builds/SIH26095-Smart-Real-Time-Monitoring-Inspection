
(function(){
"use strict";

const DB_KEY="inspectionPortalDB_v2";
const USER_KEY="inspectionPortalUser_v2";

const emptyDB={
  inspections:[],
  evidence:[],
  issues:[],
  reinspections:[],
  vcRequests:[],
  users:[],
  notifications:[],
  logs:[],
  activities:[],
  locationReviews:[],
  supportRequests:[]
};

function db(){
  try{
    const raw=localStorage.getItem(DB_KEY);
    if(!raw){saveDB(structuredClone(emptyDB));return structuredClone(emptyDB);}
    return JSON.parse(raw);
  }catch(e){console.warn("Storage error",e);return structuredClone(emptyDB)}
}
function saveDB(data){localStorage.setItem(DB_KEY,JSON.stringify(data))}
function now(){return new Date().toLocaleString("en-IN",{dateStyle:"medium",timeStyle:"short"})}
function esc(v){return String(v??"").replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;").replaceAll('"',"&quot;").replaceAll("'","&#039;")}
function id(prefix){return prefix+"-"+new Date().getFullYear()+"-"+String(Date.now()).slice(-7)}
function log(event,detail){
  const d=db();
  d.logs.unshift({time:now(),event,detail});
  d.activities.unshift({time:now(),event,detail});
  saveDB(d);
}
function notify(title,message,type="info"){
  const d=db();d.notifications.unshift({id:id("N"),title,message,type,time:now(),read:false});saveDB(d);
}
function toast(message){
  let el=document.querySelector("#portalToast");
  if(!el){el=document.createElement("div");el.id="portalToast";document.body.appendChild(el)}
  el.textContent=message;el.className="portal-toast";
  clearTimeout(window.__toastTimer);window.__toastTimer=setTimeout(()=>el.remove(),2800);
}
function getUser(){return localStorage.getItem(USER_KEY)||"Supervisor"}
function setUser(name){localStorage.setItem(USER_KEY,name)}

function updateHeaderUser(){
  document.querySelectorAll("[data-user-name]").forEach(el=>el.textContent=getUser());
}

function counts(){
  const d=db(), i=d.inspections;
  const c=(s)=>i.filter(x=>x.status===s).length;
  return {total:i.length,inProgress:c("In Progress"),underReview:c("Under Review"),actionRequired:c("Action Required"),overdue:c("Overdue"),completed:c("Closed"),critical:d.issues.filter(x=>x.severity==="Critical"&&x.status!=="Verified").length};
}

function initDashboard(){
  const d=db(), c=counts();
  const map={totalInspections:c.total,inProgressInspections:c.inProgress,underReviewInspections:c.underReview,actionRequiredInspections:c.actionRequired,overdueInspections:c.overdue,criticalIssues:c.critical};
  Object.entries(map).forEach(([id,val])=>{const el=document.getElementById(id);if(el)el.textContent=val||0});
  const bars=document.getElementById("lifecycleBars");
  if(bars){
    const statuses=["Assigned","In Progress","Submitted","Under Review","Action Required","Closed","Overdue"];
    bars.innerHTML=statuses.map(s=>{
      const n=d.inspections.filter(x=>x.status===s).length;
      const width=c.total?Math.round(n/c.total*100):0;
      return `<div class="lifecycle-row"><span>${esc(s)}</span><div class="bar-track"><div class="bar-fill" style="width:${width}%"></div></div><b>${n}</b></div>`;
    }).join("");
  }
  const queue=document.getElementById("reviewQueue");
  if(queue){
    const rows=d.inspections.filter(x=>["Submitted","Under Review","Action Required"].includes(x.status));
    queue.className=rows.length?"queue-list":"empty-state";
    queue.innerHTML=rows.length?rows.slice(0,6).map(x=>`<div class="activity-entry"><strong>${esc(x.id)}</strong><p>${esc(x.site)} · ${esc(x.status)}</p><a href="05-inspection-detail.html?id=${encodeURIComponent(x.id)}">Open record</a></div>`).join(""):`<strong>No review records available</strong><p>Submitted inspections will appear here when data is available.</p>`;
  }
  const activity=document.getElementById("dashboardActivity");
  if(activity){
    activity.className=d.activities.length?"activity-list":"activity-list empty-state";
    activity.innerHTML=d.activities.length?d.activities.slice(0,8).map(a=>`<div class="activity-entry"><strong>${esc(a.event)}</strong><p>${esc(a.detail)}</p><time>${esc(a.time)}</time></div>`):`<strong>No recent activity</strong><p>Activity will appear after a portal action is recorded.</p>`;
  }
}

function renderInspectionTable(){
  const body=document.getElementById("inspectionTableBody"); if(!body)return;
  const d=db();let rows=[...d.inspections];
  const q=(document.getElementById("inspectionSearch")?.value||"").trim().toLowerCase();
  const status=(document.getElementById("statusFilter")?.value||"").toLowerCase();
  const priority=(document.getElementById("priorityFilter")?.value||"").toLowerCase();
  const type=(document.getElementById("typeFilter")?.value||"").toLowerCase();
  if(q)rows=rows.filter(x=>[x.id,x.site,x.location,x.inspector,x.type].some(v=>String(v||"").toLowerCase().includes(q)));
  if(status)rows=rows.filter(x=>x.status.toLowerCase()===status);
  if(priority)rows=rows.filter(x=>x.priority.toLowerCase()===priority);
  if(type)rows=rows.filter(x=>x.type.toLowerCase()===type);
  if(!rows.length){body.innerHTML=`<tr><td colspan="8" class="empty-state"><strong>No inspection records match the current filters</strong><p>Clear the filters or create a new inspection assignment.</p></td></tr>`;return}
  body.innerHTML=rows.map(x=>`<tr>
<td><strong>${esc(x.id)}</strong></td><td>${esc(x.site)}<br><span style="color:#899299">${esc(x.location)}</span></td>
<td>${esc(x.inspector||"Unassigned")}</td><td>${esc(x.type)}</td><td>${esc(x.status)}</td><td>${esc(x.priority)}</td><td>${esc(x.progress)}%</td>
<td class="action-links"><a class="text-button" href="05-inspection-detail.html?id=${encodeURIComponent(x.id)}">Open</a>${x.status!=="Closed"?`<button class="text-button" data-action="advance-status" data-id="${esc(x.id)}">Advance</button>`:""}</td>
</tr>`).join("");
}

function initInspectionRegister(){
  if(!document.getElementById("inspectionTableBody"))return;
  ["inspectionSearch","statusFilter","priorityFilter","typeFilter"].forEach(id=>{
    document.getElementById(id)?.addEventListener("input",renderInspectionTable);
    document.getElementById(id)?.addEventListener("change",renderInspectionTable);
  });
  renderInspectionTable();
}

function createInspection(e){
  e.preventDefault();
  const f=new FormData(e.currentTarget);
  const mode=f.get("assignmentMode");
  const inspector=(f.get("inspector")||"").trim();
  if(mode==="Manual" && !inspector){toast("Enter an inspector for manual assignment.");return}
  const d=db();
  const item={
    id:id("INS"),type:f.get("type"),priority:f.get("priority"),site:f.get("site"),
    location:f.get("location"),inspector:inspector||"Pending selection",dueDate:f.get("dueDate")||"",
    scope:f.get("scope")||"",instructions:f.get("instructions")||"",assignmentMode:mode,
    status:"Assigned",progress:0,createdAt:now(),updatedAt:now()
  };
  d.inspections.unshift(item);saveDB(d);log("Inspection created",`${item.id} was created using ${mode}.`);notify("Inspection created",`${item.id} is now assigned.`,"success");
  e.currentTarget.reset();document.querySelector('input[name="assignmentMode"][value="Manual"]')?.setAttribute("checked","checked");toast("Inspection assignment created.");setTimeout(()=>location.href="03-inspections.html",500);
}

function detail(){
  const root=document.getElementById("detailContent");if(!root)return;
  const params=new URLSearchParams(location.search);const selected=params.get("id");const d=db();const x=d.inspections.find(i=>i.id===selected);
  if(!x){return}
  document.getElementById("detailTitle").textContent=x.id;
  document.getElementById("detailSubtitle").textContent=`${x.site} · ${x.location}`;
  root.innerHTML=`<section class="record-header"><div><h2>${esc(x.site)}</h2><p>${esc(x.scope||"No scope recorded.")}</p></div><div><strong>${esc(x.status)}</strong><p>${esc(x.priority)} priority</p></div></section>
<section class="panel"><div class="record-meta">
<div class="meta-box"><span>Inspection ID</span><strong>${esc(x.id)}</strong></div><div class="meta-box"><span>Type</span><strong>${esc(x.type)}</strong></div><div class="meta-box"><span>Inspector</span><strong>${esc(x.inspector)}</strong></div><div class="meta-box"><span>Due date</span><strong>${esc(x.dueDate||"—")}</strong></div><div class="meta-box"><span>Location</span><strong>${esc(x.location)}</strong></div><div class="meta-box"><span>Progress</span><strong>${esc(x.progress)}%</strong></div><div class="meta-box"><span>Created</span><strong>${esc(x.createdAt)}</strong></div><div class="meta-box"><span>Updated</span><strong>${esc(x.updated)}</strong></div>
</div></section>
<section class="panel"><div class="panel-header"><div><h2>Supervisor actions</h2><p>Move the record through the lifecycle as review evidence becomes available.</p></div></div><div class="detail-actions">
${["In Progress","Submitted","Under Review","Action Required","Closed"].map(s=>`<button class="secondary-button" data-action="set-status" data-id="${esc(x.id)}" data-status="${esc(s)}">${esc(s)}</button>`).join("")}
<a class="secondary-button" href="06-evidence.html">Evidence</a><a class="secondary-button" href="07-location-review.html">Location</a><a class="secondary-button" href="08-issues.html">Issues</a></div></section>`;
}

function setStatus(idValue,status){
  const d=db();const x=d.inspections.find(i=>i.id===idValue);if(!x)return;
  x.status=status;x.updated=now();x.progress=status==="Closed"?100:status==="Assigned"?0:Math.max(x.progress,Math.min(95,["In Progress","Submitted","Under Review","Action Required"].indexOf(status)+1)*20));
  saveDB(d);log("Inspection status changed",`${idValue} moved to ${status}.`);notify("Inspection updated",`${idValue} is now ${status}.`,"info");toast("Inspection status updated.");renderInspectionTable();detail();initDashboard();
}

function initEvidence(){
  const body=document.getElementById("evidenceTableBody");if(!body)return;
  const form=document.getElementById("evidenceForm");
  form?.addEventListener("submit",e=>{
    e.preventDefault();const f=new FormData(form);const d=db();
    const file=document.getElementById("eFile")?.files?.[0];
    const item={id:id("EVD"),inspectionId:f.get("inspectionId"),type:f.get("type"),fileName:file?.name||"No file attached",description:f.get("description")||"",status:"Pending Review",captured:now()};
    d.evidence.unshift(item);saveDB(d);log("Evidence record added",`${item.id} was linked to ${item.inspectionId}.`);notify("Evidence added",`${item.id} is pending review.`,"info");form.reset();toast("Evidence record saved.");renderEvidence();
  });
  renderEvidence();
}
function renderEvidence(){const body=document.getElementById("evidenceTableBody");if(!body)return;const d=db();if(!d.evidence.length){body.innerHTML=`<tr><td colspan="6" class="empty-state"><strong>No evidence records</strong><p>Saved evidence will appear here.</p></td></tr>`;return}body.innerHTML=d.evidence.map(x=>`<tr><td>${esc(x.id)}</td><td>${esc(x.inspectionId)}</td><td>${esc(x.type)}</td><td>${esc(x.fileName)}</td><td>${esc(x.status)}</td><td>${esc(x.captured)}</td></tr>`).join("")}

function initIssues(){
  const body=document.getElementById("issuesTableBody");if(!body)return;
  document.getElementById("issueForm")?.addEventListener("submit",e=>{
    e.preventDefault();const f=new FormData(e.currentTarget);const d=db();
    const item={id:id("ISS"),inspectionId:f.get("inspectionId"),severity:f.get("severity"),title:f.get("title"),description:f.get("description"),action:f.get("action")||"",dueDate:f.get("dueDate")||"",status:"Open",createdAt:now()};
    d.issues.unshift(item);saveDB(d);log("Issue recorded",`${item.id} was recorded for ${item.inspectionId}.`);notify("Issue recorded",`${item.id} requires review.`,item.severity==="Critical"?"warning":"info");e.currentTarget.reset();toast("Issue recorded.");renderIssues();
  });
  renderIssues();
}
function renderIssues(){const body=document.getElementById("issuesTableBody");if(!body)return;const d=db();if(!d.issues.length){body.innerHTML=`<tr><td colspan="7" class="empty-state"><strong>No issues recorded</strong><p>Recorded findings will appear here.</p></td></tr>`;return}body.innerHTML=d.issues.map(x=>`<tr><td><strong>${esc(x.id)}</strong><br>${esc(x.title)}</td><td>${esc(x.inspectionId)}</td><td>${esc(x.severity)}</td><td>${esc(x.status)}</td><td>${esc(x.action||"—")}</td><td>${esc(x.dueDate||"—")}</td><td><button class="text-button" data-action="verify-issue" data-id="${esc(x.id)}">${x.status==="Verified"?"Verified":"Mark verified"}</button></td></tr>`).join("")}

function verifyIssue(issueId){
  const d=db();const x=d.issues.find(i=>i.id===issueId);if(!x)return;x.status="Verified";x.verifiedAt=now();saveDB(d);log("Issue verified",`${issueId} was marked verified.`);notify("Issue verified",`${issueId} is marked verified.`,"success");renderIssues();initDashboard();
}

function initReinspection(){
  const body=document.getElementById("reinspectionTableBody");if(!body)return;
  document.getElementById("reinspectionForm")?.addEventListener("submit",e=>{
    e.preventDefault();const f=new FormData(e.currentTarget);const d=db();const x=d.inspections.find(i=>i.id===f.get("inspectionId"));
    if(!x){toast("Inspection ID not found.");return}
    const item={id:id("REI"),inspectionId:x.id,reason:f.get("reason"),dueDate:f.get("dueDate")||"",status:"Scheduled",createdAt:now()};
    d.reinspections.unshift(item);x.status="Assigned";x.progress=0;x.updated=now();saveDB(d);log("Re-inspection scheduled",`${item.id} was created for ${x.id}.`);notify("Re-inspection scheduled",`${x.id} was returned to an inspection cycle.`,"info");e.currentTarget.reset();toast("Re-inspection scheduled.");renderReinspection();
  });
  renderReinspection();
}
function renderReinspection(){const body=document.getElementById("reinspectionTableBody");if(!body)return;const d=db();if(!d.reinspections.length){body.innerHTML=`<tr><td colspan="4" class="empty-state"><strong>No re-inspection records</strong><p>Scheduled re-inspections will appear here.</p></td></tr>`;return}body.innerHTML=d.reinspections.map(x=>`<tr><td>${esc(x.inspectionId)}</td><td>${esc(x.reason)}</td><td>${esc(x.dueDate||"—")}</td><td>${esc(x.status)}</td></tr>`).join("")}

function initLocation(){
  document.getElementById("locationLookup")?.addEventListener("submit",e=>{
    e.preventDefault();const idValue=document.getElementById("locationInspectionId").value.trim();const d=db();const x=d.inspections.find(i=>i.id===idValue);
    const box=document.getElementById("locationResult");document.getElementById("locationDecisionInspection").value=idValue;
    if(!x){box.innerHTML=`<div class="empty-state"><strong>Inspection not found</strong><p>No local record matches ${esc(idValue)}.</p></div>`;return}
    box.innerHTML=`<div class="record-meta"><div class="meta-box"><span>Inspection</span><strong>${esc(x.id)}</strong></div><div class="meta-box"><span>Assigned location</span><strong>${esc(x.location)}</strong></div><div class="meta-box"><span>Latitude</span><strong>—</strong></div><div class="meta-box"><span>Longitude</span><strong>—</strong></div><div class="meta-box"><span>GPS accuracy</span><strong>—</strong></div><div class="meta-box"><span>Captured at</span><strong>—</strong></div></div><div class="empty-state"><strong>GPS data not connected</strong><p>Coordinates must come from the field inspection service before location verification can be completed.</p></div>`;
  });
  document.getElementById("locationDecisionForm")?.addEventListener("submit",e=>{
    e.preventDefault();const f=new FormData(e.currentTarget);const d=db();d.locationReviews.unshift({inspectionId:f.get("inspectionId"),decision:f.get("decision"),remarks:f.get("remarks")||"",reviewedAt:now()});saveDB(d);log("Location review recorded",`${f.get("inspectionId")} was marked ${f.get("decision")}.`);notify("Location review saved",`${f.get("inspectionId")} location review is recorded.`,"info");e.currentTarget.reset();toast("Location review saved.");
  });
}

function initVC(){
  const list=document.getElementById("vcList");if(!list)return;
  document.getElementById("vcForm")?.addEventListener("submit",e=>{
    e.preventDefault();const f=new FormData(e.currentTarget);const d=db();const item={id:id("VC"),inspectionId:f.get("inspectionId"),reason:f.get("reason"),note:f.get("note")||"",status:"Requested",createdAt:now()};d.vcRequests.unshift(item);saveDB(d);log("VC request created",`${item.id} was created for ${item.inspectionId}.`);notify("VC request created",`${item.id} is awaiting authorised meeting service.`,"info");e.currentTarget.reset();toast("VC request created.");renderVC();
  });
  renderVC();
}
function renderVC(){const list=document.getElementById("vcList");const d=db();if(!d.vcRequests.length){list.className="empty-state";list.innerHTML="<strong>No VC requests</strong><p>Created requests will appear here.</p>";return}list.className="notification-list";list.innerHTML=d.vcRequests.map(x=>`<div class="notification-card"><strong>${esc(x.id)} · ${esc(x.status)}</strong><p>Inspection: ${esc(x.inspectionId)} · ${esc(x.reason)}</p><small>${esc(x.createdAt)}</small></div>`).join("")}

function initNotifications(){
  const list=document.getElementById("notificationList");if(!list)return;renderNotifications();
}
function renderNotifications(){const list=document.getElementById("notificationList");if(!list)return;const d=db();if(!d.notifications.length){list.className="notification-list empty-state";list.innerHTML="<strong>No notifications</strong><p>New notifications will appear here.</p>";return}list.className="notification-list";list.innerHTML=d.notifications.map(x=>`<div class="notification-card ${x.read?"":"unread"}"><strong>${esc(x.title)}</strong><p>${esc(x.message)}</p><small>${esc(x.time)}</small></div>`).join("")}
function markRead(){const d=db();d.notifications=d.notifications.map(x=>({...x,read:true}));saveDB(d);renderNotifications();toast("Notifications marked as read.")}

function csvDownload(name,rows){
  const content=rows.map(r=>r.map(v=>`"${String(v??"").replaceAll('"','""')}"`).join(",")).join("\n");
  const blob=new Blob([content],{type:"text/csv;charset=utf-8"});const url=URL.createObjectURL(blob);const a=document.createElement("a");a.href=url;a.download=name;a.click();URL.revokeObjectURL(url);
}
function exportInspections(){const d=db();csvDownload("inspection-register.csv",[["ID","Type","Site","Location","Inspector","Priority","Status","Progress","Due Date"],...d.inspections.map(x=>[x.id,x.type,x.site,x.location,x.inspector,x.priority,x.status,x.progress+"%",x.dueDate])]);log("Inspection export",`${d.inspections.length} inspection rows exported.`)}
function exportIssues(){const d=db();csvDownload("issue-register.csv",[["ID","Inspection","Title","Severity","Status","Action","Due Date"],...d.issues.map(x=>[x.id,x.inspectionId,x.title,x.severity,x.status,x.action,x.dueDate])]);log("Issue export",`${d.issues.length} issue rows exported.`)}
function exportLogs(){const d=db();csvDownload("system-logs.csv",[["Time","Event","Detail"],...d.logs.map(x=>[x.time,x.event,x.detail])])}

function initAnalytics(){
  const d=db();const c=counts();
  const map={total:c.total,completed:c.completed,inprogress:c.inProgress,overdue:c.overdue};
  Object.entries(map).forEach(([k,v])=>{const el=document.querySelector(`[data-analytics="${k}"]`);if(el)el.textContent=v});
  const render=(target,labels,field)=>{
    const wrap=document.getElementById(target);if(!wrap)return;const max=Math.max(1,...labels.map(l=>d.inspections.filter(x=>x[field]===l).length));wrap.innerHTML=labels.map(l=>{const n=d.inspections.filter(x=>x[field]===l).length;return `<div class="analytics-row"><span>${esc(l)}</span><div class="analytics-track"><div class="analytics-fill" style="width:${Math.round(n/max*100)}%"></div></div><b>${n}</b></div>`}).join("");
  };
  render("analyticsStatus",["Assigned","In Progress","Submitted","Under Review","Action Required","Closed","Overdue"],"status");
  render("analyticsPriority",["Normal","High","Critical"],"priority");
}

function initUsers(){
  const body=document.getElementById("usersTableBody");if(!body)return;
  document.getElementById("userForm")?.addEventListener("submit",e=>{e.preventDefault();const f=new FormData(e.currentTarget);const d=db();d.users.unshift({name:f.get("name"),role:f.get("role"),email:f.get("email")||"",region:f.get("region")||"",status:"Active",createdAt:now()});saveDB(d);log("User entry added",`${f.get("name")} was added to the local directory.`);toast("User entry added.");e.currentTarget.reset();renderUsers()});
  renderUsers();
}
function renderUsers(){const body=document.getElementById("usersTableBody");if(!body)return;const d=db();if(!d.users.length){body.innerHTML=`<tr><td colspan="5" class="empty-state"><strong>No user entries</strong><p>Add a directory entry to test the UI.</p></td></tr>`;return}body.innerHTML=d.users.map(x=>`<tr><td>${esc(x.name)}</td><td>${esc(x.role)}</td><td>${esc(x.email||"—")}</td><td>${esc(x.region||"—")}</td><td>${esc(x.status)}</td></tr>`).join("")}

function initLogs(){
  const body=document.getElementById("logsTableBody");if(!body)return;const d=db();if(!d.logs.length){body.innerHTML=`<tr><td colspan="3" class="empty-state"><strong>No log events</strong><p>Front-end actions will appear here.</p></td></tr>`;return}body.innerHTML=d.logs.slice(0,100).map(x=>`<tr><td>${esc(x.time)}</td><td>${esc(x.event)}</td><td>${esc(x.detail)}</td></tr>`).join("")
}

function initSupport(){
  const form=document.getElementById("supportForm");if(!form)return;form.addEventListener("submit",e=>{e.preventDefault();const f=new FormData(form);const d=db();d.supportRequests.unshift({id:id("SUP"),subject:f.get("subject"),message:f.get("message"),createdAt:now(),status:"Submitted"});saveDB(d);log("Support request submitted",`${f.get("subject")} was submitted.`);notify("Support request submitted",`${f.get("subject")} was recorded.`,"success");form.reset();toast("Support request submitted.")})
}

function initLogin(){
  const form=document.getElementById("loginForm");if(!form)return;form.addEventListener("submit",e=>{e.preventDefault();const f=new FormData(form);setUser((f.get("username")||"Supervisor").trim());location.href="02-index.html"})
}

function initLive(){
  document.querySelector("[data-action='request-live-feed']")?.addEventListener("click",()=>{const idv=document.getElementById("liveInspectionId")?.value.trim();if(!idv){toast("Enter an inspection ID first.");return}log("Live feed request",`Authorised feed request initiated for ${idv}.`);notify("Live feed requested",`Feed request recorded for ${idv}.`,"info");toast("Live feed request recorded.")})
}

document.addEventListener("click",e=>{
  const b=e.target.closest("[data-action]");if(!b)return;
  const action=b.dataset.action;
  if(action==="toggle-sidebar")document.querySelector(".sidebar")?.classList.toggle("sidebar-open");
  if(action==="logout"){localStorage.removeItem(USER_KEY);location.href="01-login.html"}
  if(action==="clear-inspection-filters"){["inspectionSearch","statusFilter","priorityFilter","typeFilter"].forEach(i=>{const el=document.getElementById(i);if(el)el.value=""});renderInspectionTable()}
  if(action==="export-inspections")exportInspections();
  if(action==="export-issues")exportIssues();
  if(action==="export-logs")exportLogs();
  if(action==="mark-notifications-read")markRead();
  if(action==="advance-status"){const x=db().inspections.find(i=>i.id===b.dataset.id);if(x){const order=["Assigned","In Progress","Submitted","Under Review","Action Required","Closed"];const next=order[Math.min(order.indexOf(x.status)+1,order.length-1)];setStatus(x.id,next)}}
  if(action==="set-status")setStatus(b.dataset.id,b.dataset.status);
  if(action==="verify-issue")verifyIssue(b.dataset.id);
});

document.addEventListener("DOMContentLoaded",()=>{
  updateHeaderUser();initLogin();initDashboard();initInspectionRegister();document.getElementById("inspectionAssignmentForm")?.addEventListener("submit",createInspection);
  detail();initEvidence();initIssues();initReinspection();initLocation();initVC();initNotifications();initAnalytics();initUsers();initLogs();initSupport();initLive();
});
})();
