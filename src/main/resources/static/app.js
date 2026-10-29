const $ = id => document.getElementById(id);
const labels = {HIGH: "High", MEDIUM: "Medium", LOW: "Low"};

async function api(path, method = "GET", body) {
  const res = await fetch("/api" + path, {
    method,
    headers: body ? {"Content-Type": "application/json"} : {},
    body: body ? JSON.stringify(body) : undefined
  });
  if (!res.ok) {
    let msg = "Request failed";
    try { msg = (await res.json()).message || msg; } catch (e) {}
    throw new Error(msg);
  }
  return res.status === 204 ? null : res.json();
}

function el(tag, cls, text) {
  const e = document.createElement(tag);
  if (cls) e.className = cls;
  if (text !== undefined) e.textContent = text;
  return e;
}

async function load() {
  const p = new URLSearchParams({q: $("q").value, status: $("status").value, priority: $("fpriority").value});
  const [tasks, all] = await Promise.all([api("/tasks?" + p), api("/tasks")]);
  const done = all.filter(t => t.completed).length;
  $("s-total").textContent = all.length;
  $("s-pending").textContent = all.length - done;
  $("s-done").textContent = done;
  const list = $("list");
  list.replaceChildren();
  $("empty").hidden = tasks.length > 0;
  tasks.forEach(t => list.appendChild(row(t)));
}

function row(t) {
  const li = el("li", t.priority + (t.completed ? " done" : ""));
  const cb = el("input"); cb.type = "checkbox"; cb.checked = t.completed;
  cb.setAttribute("aria-label", "Mark " + t.title + " completed");
  cb.onchange = () => api("/tasks/" + t.id + "/complete", "PATCH").then(load);
  const body = el("div", "body");
  body.append(el("div", "t", t.title));
  if (t.description) body.append(el("div", "d", t.description));
  body.append(el("div", "meta", labels[t.priority] + " priority" + (t.dueDate ? ", due " + t.dueDate : "")));
  const edit = el("button", "ghost", "Edit"); edit.onclick = () => startEdit(t);
  const del = el("button", "danger", "Delete");
  del.onclick = () => api("/tasks/" + t.id, "DELETE").then(load);
  li.append(cb, body, edit, del);
  return li;
}

function startEdit(t) {
  $("id").value = t.id; $("title").value = t.title; $("description").value = t.description;
  $("priority").value = t.priority; $("dueDate").value = t.dueDate;
  $("save").textContent = "Save changes"; $("cancel").hidden = false;
  $("title").focus();
}

function resetForm() {
  $("form").reset(); $("id").value = "";
  $("save").textContent = "Add task"; $("cancel").hidden = true; $("error").textContent = "";
}

$("form").onsubmit = async e => {
  e.preventDefault();
  const body = {title: $("title").value, description: $("description").value,
                priority: $("priority").value, dueDate: $("dueDate").value};
  try {
    const id = $("id").value;
    await (id ? api("/tasks/" + id, "PUT", body) : api("/tasks", "POST", body));
    resetForm(); load();
  } catch (err) { $("error").textContent = err.message; }
};
$("cancel").onclick = resetForm;
["q", "status", "fpriority"].forEach(id => $(id).addEventListener("input", load));
load();
