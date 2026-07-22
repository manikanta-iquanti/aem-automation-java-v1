(() => {
  const state = {
    selectedId: null,
    readiness: null,
  };

  const $ = (id) => document.getElementById(id);

  function esc(value) {
    return String(value == null ? "" : value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function stripHtml(html) {
    return String(html == null ? "" : html)
      .replace(/<script[\s\S]*?<\/script>/gi, " ")
      .replace(/<style[\s\S]*?<\/style>/gi, " ")
      .replace(/<[^>]+>/g, " ")
      .replace(/&nbsp;/gi, " ")
      .replace(/&amp;/gi, "&")
      .replace(/&lt;/gi, "<")
      .replace(/&gt;/gi, ">")
      .replace(/&quot;/gi, "\"")
      .replace(/\s+/g, " ")
      .trim();
  }

  function previewSample(raw) {
    const plain = stripHtml(raw);
    if (!plain) return "";
    if (plain.length <= 160) return plain;
    return plain.slice(0, 160).trim() + "…";
  }

  async function api(path, options = {}) {
    const res = await fetch(path, options);
    const text = await res.text();
    let data = null;
    try { data = text ? JSON.parse(text) : null; } catch { data = { error: text }; }
    if (!res.ok) {
      throw new Error((data && data.error) || res.statusText || "Request failed");
    }
    return data;
  }

  function showTab(name) {
    document.querySelectorAll(".tab").forEach((t) => {
      t.classList.toggle("active", t.dataset.tab === name);
    });
    document.querySelectorAll(".panel").forEach((p) => {
      p.classList.toggle("active", p.id === `panel-${name}`);
    });
  }

  document.querySelectorAll(".tab").forEach((tab) => {
    tab.addEventListener("click", () => {
      if (tab.disabled) return;
      showTab(tab.dataset.tab);
    });
  });

  function clearEditor() {
    state.selectedId = null;
    document.querySelectorAll("#blueprintList li").forEach((li) => {
      li.classList.remove("selected");
    });
    $("bpName").value = "";
    $("bpJson").value = "";
    $("bpJsonPaste").value = "";
    $("bpZip").value = "";
    $("derivedBox").classList.add("muted");
    $("derivedBox").textContent = "Upload a package to derive paths.";
    $("contentParentPath").value = "";
    $("samplePageName").value = "";
    $("packageName").value = "";
    $("vaultPackageDir").value = "";
    $("advToggle").checked = false;
    $("advFields").hidden = true;
    $("picker").classList.add("muted");
    $("picker").textContent = "Select a blueprint to load components.";
    $("saveFields").disabled = true;
    $("downloadTemplate").hidden = true;
    $("downloadPackage").hidden = true;
    $("generateStatus").textContent = "";
    $("buildStatus").textContent = "";
    $("templatePreview").hidden = true;
    $("previewBlocks").innerHTML = "";
    $("articlesGenerateStatus").textContent = "";
    $("articlesBuildStatus").textContent = "";
    refreshReadiness();
  }

  async function refreshList() {
    const list = await api("/api/blueprints");
    const ul = $("blueprintList");
    ul.innerHTML = "";
    if (!list.length) {
      ul.innerHTML = "<li class='muted'>No blueprints yet.</li>";
      return;
    }
    list.forEach((bp) => {
      const li = document.createElement("li");
      li.dataset.id = bp.id;
      if (bp.id === state.selectedId) li.classList.add("selected");
      li.innerHTML = `<strong>${esc(bp.id)}</strong><span class="meta">${esc(String(bp.fieldCount || 0))} fields · ${esc(bp.package?.samplePageName || "")}</span>`;
      li.addEventListener("click", () => {
        if (state.selectedId === bp.id) {
          clearEditor();
          return;
        }
        selectBlueprint(bp.id, bp);
      });
      ul.appendChild(li);
    });
  }

  $("refreshList").addEventListener("click", refreshList);
  $("newBlueprint").addEventListener("click", clearEditor);

  async function selectBlueprint(id, summary) {
    state.selectedId = id;
    document.querySelectorAll("#blueprintList li").forEach((li) => {
      li.classList.toggle("selected", li.dataset.id === id);
    });
    $("bpName").value = id;
    $("bpJson").value = "";
    $("bpJsonPaste").value = "";
    $("bpZip").value = "";
    if (summary && summary.package) {
      fillDerived(summary.package);
    }
    await Promise.all([loadPicker(id), refreshReadiness(), loadExistingPreview(id)]);
  }

  async function loadExistingPreview(id) {
    try {
      const preview = await api(`/api/blueprints/${id}/template-preview`);
      renderTemplatePreview(preview.blocks || []);
      const link = $("downloadTemplate");
      link.hidden = false;
      link.href = `/api/blueprints/${id}/download/template`;
      $("generateStatus").textContent = "Existing template: " + preview.path;
    } catch {
      $("templatePreview").hidden = true;
      $("previewBlocks").innerHTML = "";
      $("downloadTemplate").hidden = true;
    }
  }

  function fillDerived(pkg) {
    $("derivedBox").classList.remove("muted");
    $("derivedBox").textContent =
      `contentParentPath: ${pkg.contentParentPath}\n` +
      `samplePageName:     ${pkg.samplePageName}\n` +
      `packageName:        ${pkg.packageName}\n` +
      `vaultPackageDir:    ${pkg.vaultPackageDir}`;
    $("contentParentPath").value = pkg.contentParentPath || "";
    $("samplePageName").value = pkg.samplePageName || "";
    $("packageName").value = pkg.packageName || "";
    $("vaultPackageDir").value = pkg.vaultPackageDir || "";
  }

  $("advToggle").addEventListener("change", (e) => {
    $("advFields").hidden = !e.target.checked;
  });

  $("bpJson").addEventListener("change", () => {
    if ($("bpJson").files[0]) {
      $("bpJsonPaste").value = "";
    }
  });

  $("bpJsonPaste").addEventListener("input", () => {
    if ($("bpJsonPaste").value.trim()) {
      $("bpJson").value = "";
    }
  });

  $("createBp").addEventListener("click", async () => {
    const jsonFile = $("bpJson").files[0];
    const jsonPaste = $("bpJsonPaste").value.trim();
    const zip = $("bpZip").files[0];
    if ((!jsonFile && !jsonPaste) || !zip) {
      alert("Provide blueprint JSON (file or paste) and a FileVault zip.");
      return;
    }
    if (jsonPaste) {
      try {
        JSON.parse(jsonPaste);
      } catch {
        alert("Pasted content is not valid JSON.");
        return;
      }
    }
    const fd = new FormData();
    fd.append("name", $("bpName").value || (jsonFile ? jsonFile.name.replace(/\.json$/i, "") : "blueprint"));
    if (jsonFile) {
      fd.append("json", jsonFile);
    } else {
      fd.append("jsonText", jsonPaste);
    }
    fd.append("zip", zip);
    try {
      const created = await api("/api/blueprints", { method: "POST", body: fd });
      fillDerived(created.derived);
      await refreshList();
      await selectBlueprint(created.id, { package: created.derived });
      alert(created.hint || "Blueprint created.");
    } catch (e) {
      alert(e.message);
    }
  });

  $("savePackage").addEventListener("click", async () => {
    if (!state.selectedId) return;
    await api(`/api/blueprints/${state.selectedId}/package`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        contentParentPath: $("contentParentPath").value,
        samplePageName: $("samplePageName").value,
        packageName: $("packageName").value,
        vaultPackageDir: $("vaultPackageDir").value,
      }),
    });
    fillDerived({
      contentParentPath: $("contentParentPath").value,
      samplePageName: $("samplePageName").value,
      packageName: $("packageName").value,
      vaultPackageDir: $("vaultPackageDir").value,
    });
    await refreshReadiness();
  });

  async function loadPicker(id) {
    const tree = await api(`/api/blueprints/${id}/components`);
    const box = $("picker");
    box.classList.remove("muted");
    box.innerHTML = "";

    (tree.pageFields || []).forEach((pf) => {
      box.appendChild(renderPageField(pf));
    });

    (tree.components || []).forEach((comp) => {
      box.appendChild(renderComponent(comp));
    });

    if (!(tree.components || []).length) {
      const empty = document.createElement("p");
      empty.className = "muted";
      empty.textContent = "No authorable components found (containers are hidden).";
      box.appendChild(empty);
    }
    $("saveFields").disabled = false;
  }

  function renderPageField(pf) {
    const row = document.createElement("div");
    row.className = "component";
    row.innerHTML = `<h4>Page fields</h4>`;
    const propRow = document.createElement("div");
    propRow.className = "prop-row";
    propRow.dataset.page = "1";
    propRow.dataset.property = pf.property;
    propRow.innerHTML = `
      <input type="checkbox" ${pf.selected ? "checked" : ""} />
      <div class="prop-meta">
        <span class="prop-name">${esc(pf.property)}</span>
      </div>
      <select>
        <option value="PLAIN">PLAIN</option>
        <option value="HTML">HTML</option>
        <option value="LIST">LIST</option>
      </select>`;
    propRow.querySelector("select").value = pf.format || "PLAIN";
    row.appendChild(propRow);
    return row;
  }

  function renderComponent(comp) {
    const el = document.createElement("div");
    el.className = "component";
    el.dataset.resourceType = comp.resourceType;
    el.dataset.path = comp.path;

    const header = document.createElement("div");
    header.className = "comp-header";
    const compCheck = document.createElement("input");
    compCheck.type = "checkbox";
    compCheck.className = "comp-check";
    compCheck.checked = !!comp.selected;
    compCheck.indeterminate = !!comp.partial && !comp.selected;
    header.appendChild(compCheck);

    const titles = document.createElement("div");
    titles.className = "comp-titles";
    titles.innerHTML = `<h4>${esc(comp.name || "(component)")}</h4>
      <div class="rt">${esc(comp.resourceType)}<br>${esc(comp.path)}</div>`;
    header.appendChild(titles);
    el.appendChild(header);

    (comp.properties || []).forEach((prop) => {
      el.appendChild(renderProp(comp.resourceType, comp.path, prop));
    });

    compCheck.addEventListener("change", () => {
      el.querySelectorAll(".prop-row input[type='checkbox']").forEach((cb) => {
        cb.checked = compCheck.checked;
      });
      syncComponentCheck(el);
    });

    el.querySelectorAll(".prop-row input[type='checkbox']").forEach((cb) => {
      cb.addEventListener("change", () => syncComponentCheck(el));
    });

    return el;
  }

  function syncComponentCheck(el) {
    const boxes = [...el.querySelectorAll(".prop-row input[type='checkbox']")];
    const checked = boxes.filter((b) => b.checked).length;
    const compCheck = el.querySelector(".comp-check");
    if (!compCheck) return;
    compCheck.checked = checked === boxes.length && boxes.length > 0;
    compCheck.indeterminate = checked > 0 && checked < boxes.length;
  }

  function renderProp(resourceType, path, prop) {
    const row = document.createElement("div");
    row.className = "prop-row";
    row.dataset.resourceType = resourceType;
    row.dataset.path = path;
    row.dataset.property = prop.property;
    const sample = previewSample(prop.sample);
    row.innerHTML = `
      <input type="checkbox" ${prop.selected ? "checked" : ""} />
      <div class="prop-meta">
        <span class="prop-name">${esc(prop.property)}</span>
        ${sample ? `<span class="prop-sample" title="${esc(stripHtml(prop.sample))}">${esc(sample)}</span>` : ""}
      </div>
      <select>
        <option value="PLAIN">PLAIN</option>
        <option value="HTML">HTML</option>
        <option value="LIST">LIST</option>
      </select>`;
    row.querySelector("select").value = prop.format || "PLAIN";
    return row;
  }

  $("saveFields").addEventListener("click", async () => {
    if (!state.selectedId) return;
    const fields = [];
    const pageFields = [];
    $("picker").querySelectorAll(".prop-row").forEach((row) => {
      const checked = row.querySelector('input[type="checkbox"]').checked;
      if (!checked) return;
      const format = row.querySelector("select").value;
      if (row.dataset.page === "1") {
        pageFields.push({ property: row.dataset.property, format });
      } else {
        fields.push({
          resourceType: row.dataset.resourceType,
          path: row.dataset.path,
          property: row.dataset.property,
          format,
        });
      }
    });
    await api(`/api/blueprints/${state.selectedId}/fields`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ fields, pageFields }),
    });
    await refreshList();
    await refreshReadiness();
    alert("Selection saved. Template will include only chosen components/fields.");
  });

  async function refreshReadiness() {
    const hint = $("readinessHint");
    const tabGenerate = $("tabGenerate");
    const tabBuild = $("tabBuild");
    if (!state.selectedId) {
      tabGenerate.disabled = true;
      tabBuild.disabled = true;
      hint.hidden = true;
      renderArticleLists([]);
      return;
    }
    const r = await api(`/api/blueprints/${state.selectedId}/readiness`);
    state.readiness = r;
    tabGenerate.disabled = !r.generateTemplate.ready;
    // Open Build once package setup is ready — articles can be uploaded on that tab
    const buildSetupReady = !!(r.buildPackage.setupReady || r.buildPackage.ready);
    tabBuild.disabled = !buildSetupReady;

    const missing = [];
    if (!r.generateTemplate.ready) {
      missing.push("Generate needs: " + (r.generateTemplate.missing.join(", ") || "—"));
    }
    if (!r.buildPackage.ready) {
      missing.push("Build needs: " + (r.buildPackage.missing.join(", ") || "—"));
    }
    if (missing.length) {
      hint.hidden = false;
      hint.textContent = `Selected: ${state.selectedId}. ` + missing.join(" · ");
    } else {
      hint.hidden = false;
      hint.textContent = `Selected: ${state.selectedId}. Generate and Build are ready.`;
    }

    $("buildMissing").textContent = r.buildPackage.ready
      ? ""
      : "Missing: " + r.buildPackage.missing.join(", ");
    $("runBuild").disabled = !r.buildPackage.ready;
    $("runGenerate").disabled = !r.generateTemplate.ready;
    renderArticleLists(r.buildPackage.articles || []);
  }

  function renderArticleLists(names) {
    const render = (ulId) => {
      const ul = $(ulId);
      if (!ul) return;
      ul.innerHTML = "";
      if (!names.length) {
        ul.classList.add("muted");
        ul.innerHTML = "<li>No articles uploaded yet.</li>";
        return;
      }
      ul.classList.remove("muted");
      names.forEach((name) => {
        const li = document.createElement("li");
        li.textContent = name;
        ul.appendChild(li);
      });
    };
    render("articlesGenerateList");
    render("articlesBuildList");
  }

  function renderTemplatePreview(blocks) {
    const panel = $("templatePreview");
    const box = $("previewBlocks");
    box.innerHTML = "";
    if (!blocks || !blocks.length) {
      panel.hidden = true;
      return;
    }
    panel.hidden = false;
    blocks.forEach((block) => {
      const el = document.createElement("div");
      el.className = "preview-block";
      el.innerHTML = `<div class="preview-marker">${esc(block.marker || "")}</div>
        <div class="preview-value">${esc(block.value || "(empty)")}</div>`;
      box.appendChild(el);
    });
  }

  async function uploadArticlesFrom(inputId, statusId) {
    if (!state.selectedId) {
      alert("Select a blueprint first.");
      return;
    }
    const input = $(inputId);
    const files = input.files;
    if (!files.length) {
      alert("Choose at least one .docx article.");
      return;
    }
    const fd = new FormData();
    for (const f of files) fd.append("articles", f);
    $(statusId).textContent = "Uploading…";
    try {
      const result = await api(`/api/blueprints/${state.selectedId}/articles`, {
        method: "POST",
        body: fd,
      });
      $(statusId).textContent = `Uploaded ${result.articles.length} article(s).`;
      input.value = "";
      await refreshReadiness();
    } catch (e) {
      $(statusId).textContent = e.message;
    }
  }

  $("uploadArticlesGenerate").addEventListener("click", () => {
    uploadArticlesFrom("articlesGenerate", "articlesGenerateStatus");
  });
  $("uploadArticlesBuild").addEventListener("click", () => {
    uploadArticlesFrom("articles", "articlesBuildStatus");
  });

  $("runGenerate").addEventListener("click", async () => {
    if (!state.selectedId) return;
    $("generateStatus").textContent = "Generating…";
    try {
      const result = await api(`/api/blueprints/${state.selectedId}/generate-template`, {
        method: "POST",
      });
      $("generateStatus").textContent = "Template written to " + result.path;
      const link = $("downloadTemplate");
      link.hidden = false;
      link.href = `/api/blueprints/${state.selectedId}/download/template`;
      renderTemplatePreview(result.blocks || []);
    } catch (e) {
      $("generateStatus").textContent = e.message;
      $("templatePreview").hidden = true;
    }
  });

  $("runBuild").addEventListener("click", async () => {
    if (!state.selectedId) return;
    const files = $("articles").files;
    const fd = new FormData();
    for (const f of files) fd.append("articles", f);
    $("buildStatus").textContent = "Building…";
    try {
      const result = await api(`/api/blueprints/${state.selectedId}/build-package`, {
        method: "POST",
        body: fd,
      });
      $("buildStatus").textContent = "Package written to " + result.path;
      const link = $("downloadPackage");
      link.hidden = false;
      link.href = `/api/blueprints/${state.selectedId}/download/package`;
      await refreshReadiness();
    } catch (e) {
      $("buildStatus").textContent = e.message;
      await refreshReadiness();
    }
  });

  refreshList().catch((e) => alert(e.message));
})();
