(() => {
  const state = {
    selectedId: null,
    readiness: null,
    settings: {
      defaultPackageName: "bulk-content",
      showGenerateTab: false,
      showAdaptTab: false,
      showCreateBlueprint: true,
      showHelpText: false,
      aemBaseUrl: "http://localhost:4502",
      aemUsername: "admin",
      aemPassword: "admin",
    },
    adapt: {
      jobId: null,
      reviews: [],
      index: 0,
      approved: {},
      mappingId: "",
      templatePath: "",
    },
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

  function activeTabName() {
    const active = document.querySelector(".tab.active");
    return active ? active.dataset.tab : "blueprints";
  }

  function applyUiSettings(settings) {
    state.settings = {
      defaultPackageName: settings.defaultPackageName || "bulk-content",
      showGenerateTab: !!settings.showGenerateTab,
      showAdaptTab: !!settings.showAdaptTab,
      showCreateBlueprint: settings.showCreateBlueprint !== false,
      showHelpText: !!settings.showHelpText,
      aemBaseUrl: settings.aemBaseUrl || "http://localhost:4502",
      aemUsername: settings.aemUsername || "admin",
      aemPassword: settings.aemPassword != null ? settings.aemPassword : "admin",
    };

    const s = state.settings;
    document.body.classList.toggle("show-help", s.showHelpText);

    const tabGenerate = $("tabGenerate");
    const tabAdapt = $("tabAdapt");
    tabGenerate.hidden = !s.showGenerateTab;
    tabAdapt.hidden = !s.showAdaptTab;

    const createSection = $("createBlueprintSection");
    createSection.hidden = !s.showCreateBlueprint;
    $("derivedStepTitle").textContent = s.showCreateBlueprint
      ? "2. Derived package paths"
      : "1. Derived package paths";
    $("pickerStepTitle").textContent = s.showCreateBlueprint
      ? "3. Component picker"
      : "2. Component picker";

    fillSettingsFormFromState();

    const current = activeTabName();
    if ((current === "generate" && !s.showGenerateTab)
        || (current === "adapt" && !s.showAdaptTab)) {
      showTab("blueprints");
    }

    refreshReadiness().catch(() => {});
  }

  function fillSettingsFormFromState() {
    const s = state.settings;
    $("setShowGenerate").checked = s.showGenerateTab;
    $("setShowAdapt").checked = s.showAdaptTab;
    $("setShowCreate").checked = s.showCreateBlueprint;
    $("setShowHelp").checked = s.showHelpText;
    $("setDefaultPackage").value = s.defaultPackageName;
    $("setAemBaseUrl").value = s.aemBaseUrl;
    $("setAemUsername").value = s.aemUsername;
    $("setAemPassword").value = s.aemPassword;
  }

  function settingsFromForm() {
    return {
      defaultPackageName: $("setDefaultPackage").value.trim() || "bulk-content",
      showGenerateTab: $("setShowGenerate").checked,
      showAdaptTab: $("setShowAdapt").checked,
      showCreateBlueprint: $("setShowCreate").checked,
      showHelpText: $("setShowHelp").checked,
      aemBaseUrl: $("setAemBaseUrl").value.trim() || "http://localhost:4502",
      aemUsername: $("setAemUsername").value.trim() || "admin",
      aemPassword: $("setAemPassword").value,
    };
  }

  function setSettingsOpen(open) {
    $("settingsPanel").hidden = !open;
    $("settingsToggle").setAttribute("aria-expanded", open ? "true" : "false");
  }

  async function loadSettings() {
    const data = await api("/api/settings");
    applyUiSettings(data);
  }

  async function saveSettings(payload) {
    $("settingsStatus").textContent = "Saving…";
    const data = await api("/api/settings", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    applyUiSettings(data);
    $("settingsStatus").textContent = "Saved.";
  }

  document.querySelectorAll(".tab").forEach((tab) => {
    tab.addEventListener("click", () => {
      if (tab.disabled || tab.hidden) return;
      showTab(tab.dataset.tab);
    });
  });

  $("settingsToggle").addEventListener("click", () => {
    const open = $("settingsPanel").hidden;
    if (open) fillSettingsFormFromState();
    setSettingsOpen(open);
  });
  $("settingsClose").addEventListener("click", () => setSettingsOpen(false));
  $("settingsSave").addEventListener("click", () => {
    saveSettings(settingsFromForm()).catch((e) => {
      $("settingsStatus").textContent = e.message;
    });
  });
  $("presetDaily").addEventListener("click", () => {
    $("setShowGenerate").checked = false;
    $("setShowAdapt").checked = false;
    $("setShowCreate").checked = true;
    $("setShowHelp").checked = false;
  });
  $("presetFull").addEventListener("click", () => {
    $("setShowGenerate").checked = true;
    $("setShowAdapt").checked = true;
    $("setShowCreate").checked = true;
    $("setShowHelp").checked = true;
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
    if ($("bpPageUrl")) $("bpPageUrl").value = "";
    if ($("createBpStatus")) $("createBpStatus").textContent = "";
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
    loadAdaptTemplates().catch(() => {});
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

  function currentCreateMode() {
    const checked = document.querySelector('input[name="createMode"]:checked');
    return checked ? checked.value : "upload";
  }

  function applyCreateModeUi() {
    const mode = currentCreateMode();
    const aem = mode === "aem";
    $("createModeUpload").hidden = aem;
    $("createModeAem").hidden = !aem;
    $("createBp").textContent = aem ? "Fetch & derive" : "Upload & derive";
  }

  document.querySelectorAll('input[name="createMode"]').forEach((radio) => {
    radio.addEventListener("change", applyCreateModeUi);
  });
  applyCreateModeUi();

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

  async function afterBlueprintCreated(created) {
    fillDerived(created.derived);
    await refreshList();
    await selectBlueprint(created.id, { package: created.derived });
    alert(created.hint || "Blueprint created.");
  }

  $("createBp").addEventListener("click", async () => {
    $("createBpStatus").textContent = "";
    if (currentCreateMode() === "aem") {
      const pageUrl = $("bpPageUrl").value.trim();
      if (!pageUrl) {
        alert("Enter an AEM page URL or content path.");
        return;
      }
      $("createBpStatus").textContent = "Fetching from AEM…";
      $("createBp").disabled = true;
      try {
        const created = await api("/api/blueprints/from-aem", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            name: $("bpName").value.trim(),
            pageUrl,
          }),
        });
        $("createBpStatus").textContent = "Fetched and derived.";
        await afterBlueprintCreated(created);
      } catch (e) {
        $("createBpStatus").textContent = e.message;
        alert(e.message);
      } finally {
        $("createBp").disabled = false;
      }
      return;
    }

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
    $("createBpStatus").textContent = "Uploading…";
    $("createBp").disabled = true;
    try {
      const created = await api("/api/blueprints", { method: "POST", body: fd });
      $("createBpStatus").textContent = "Uploaded and derived.";
      await afterBlueprintCreated(created);
    } catch (e) {
      $("createBpStatus").textContent = e.message;
      alert(e.message);
    } finally {
      $("createBp").disabled = false;
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
    const helpOn = state.settings.showHelpText;

    if (!state.selectedId) {
      tabGenerate.disabled = true;
      tabBuild.disabled = true;
      hint.hidden = true;
      hint.textContent = "";
      renderArticleLists([]);
      return;
    }
    const r = await api(`/api/blueprints/${state.selectedId}/readiness`);
    state.readiness = r;
    tabGenerate.disabled = !r.generateTemplate.ready;
    // Open Build once package setup is ready — articles can be uploaded on that tab
    const buildSetupReady = !!(r.buildPackage.setupReady || r.buildPackage.ready);
    tabBuild.disabled = !buildSetupReady;

    if (!helpOn) {
      hint.hidden = true;
      hint.textContent = "";
    } else {
      const missing = [];
      if (state.settings.showGenerateTab && !r.generateTemplate.ready) {
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
        hint.textContent = `Selected: ${state.selectedId}. Ready.`;
      }
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

  async function loadAdaptMappings() {
    const select = $("adaptMapping");
    if (!select) return;
    try {
      const data = await api("/api/adapt/mappings");
      const ids = data.mappings || [];
      select.innerHTML = "";
      const auto = document.createElement("option");
      auto.value = "__auto__";
      auto.textContent = "Auto (detect source + bind to template)";
      select.appendChild(auto);
      ids.forEach((id) => {
        const opt = document.createElement("option");
        opt.value = id;
        opt.textContent = id;
        select.appendChild(opt);
      });
      select.value = "__auto__";
      toggleAdaptAutoFields();
    } catch (e) {
      select.innerHTML = `<option value="">${esc(e.message)}</option>`;
    }
  }

  async function loadAdaptTemplates() {
    const select = $("adaptTemplate");
    if (!select) return;
    try {
      const data = await api("/api/adapt/templates");
      const templates = data.templates || [];
      select.innerHTML = "";
      if (!templates.length) {
        select.innerHTML = '<option value="">No templates in input/templates</option>';
        return;
      }
      templates.forEach((t) => {
        const opt = document.createElement("option");
        opt.value = t.path || t.id;
        opt.textContent = t.id + (t.markers && t.markers.length ? ` (${t.markers.length} markers)` : "");
        select.appendChild(opt);
      });
      if (state.selectedId) {
        const match = templates.find((t) => t.id === state.selectedId);
        if (match) select.value = match.path || match.id;
      }
    } catch (e) {
      select.innerHTML = `<option value="">${esc(e.message)}</option>`;
    }
  }

  async function loadAdaptRecipes() {
    const select = $("adaptRecipe");
    if (!select) return;
    try {
      const data = await api("/api/adapt/recipes");
      const ids = data.recipes || ["auto"];
      select.innerHTML = "";
      ids.forEach((id) => {
        const opt = document.createElement("option");
        opt.value = id;
        opt.textContent = id === "auto" ? "Auto-detect" : id;
        select.appendChild(opt);
      });
      select.value = "auto";
    } catch (e) {
      select.innerHTML = '<option value="auto">Auto-detect</option>';
    }
  }

  function toggleAdaptAutoFields() {
    const auto = $("adaptMapping") && $("adaptMapping").value === "__auto__";
    const template = $("adaptTemplate");
    const recipe = $("adaptRecipe");
    if (template) template.disabled = !auto;
    if (recipe) recipe.disabled = !auto;
  }

  function currentAdaptReview() {
    return state.adapt.reviews[state.adapt.index] || null;
  }

  function renderAdaptReview() {
    const panel = $("adaptReview");
    const review = currentAdaptReview();
    if (!review) {
      panel.hidden = true;
      return;
    }
    panel.hidden = false;
    $("adaptIndex").textContent =
      `${state.adapt.index + 1} / ${state.adapt.reviews.length}: ${review.sourceFile}`;
    $("adaptSourcePlain").textContent = review.sourcePlainText || "(empty)";

    const mapped = $("adaptMappedSlots");
    mapped.innerHTML = "";
    (review.slots || []).forEach((slot) => {
      const el = document.createElement("div");
      el.className = "preview-block";
      el.innerHTML = `<div class="preview-marker">[[${esc(slot.path)}]]</div>
        <div class="preview-value">${esc(slot.value || "(empty)")}</div>`;
      mapped.appendChild(el);
    });

    const hint = $("adaptRecipeHint");
    if (hint) {
      const bits = [];
      if (review.recipeId) bits.push("Recipe: " + review.recipeId);
      if (review.bindStrategy) bits.push("Bind: " + review.bindStrategy);
      hint.textContent = bits.join(" · ");
    }

    const strip = $("adaptSlotStrip");
    strip.innerHTML = "";
    const units = review.units || [];
    (review.slots || []).forEach((slot) => {
      const row = document.createElement("div");
      row.className = "adapt-slot-row";
      const path = document.createElement("code");
      path.textContent = slot.path;
      row.appendChild(path);
      if (units.length) {
        const select = document.createElement("select");
        select.className = "adapt-unit-select";
        select.dataset.path = slot.path;
        const empty = document.createElement("option");
        empty.value = "__empty__";
        empty.textContent = "(leave empty)";
        select.appendChild(empty);
        units.forEach((unit) => {
          const opt = document.createElement("option");
          opt.value = unit.id;
          opt.textContent = unit.label;
          select.appendChild(opt);
        });
        select.value = slot.unitId || "__empty__";
        row.appendChild(select);
      } else {
        const excerpt = document.createElement("div");
        excerpt.className = "muted-cell";
        excerpt.textContent = slot.sourceExcerpt || "";
        row.appendChild(excerpt);
      }
      const preview = document.createElement("div");
      preview.className = "muted-cell";
      preview.textContent = previewSample(slot.value) || "(empty)";
      row.appendChild(preview);
      strip.appendChild(row);
    });

    const dl = $("adaptDownload");
    dl.href = `/api/adapt/download/${encodeURIComponent(state.adapt.jobId)}/${encodeURIComponent(review.adaptedFile)}`;
    dl.download = review.adaptedFile;

    const key = review.adaptedFile;
    $("adaptApprove").checked = !!state.adapt.approved[key];
    $("adaptPrev").disabled = state.adapt.index <= 0;
    $("adaptNext").disabled = state.adapt.index >= state.adapt.reviews.length - 1;
  }

  function collectAdaptBindings() {
    const selects = document.querySelectorAll("#adaptSlotStrip .adapt-unit-select");
    const bindings = [];
    selects.forEach((sel) => {
      bindings.push({ unit: sel.value, path: sel.dataset.path });
    });
    return bindings;
  }

  $("adaptMapping").addEventListener("change", toggleAdaptAutoFields);

  $("runAdapt").addEventListener("click", async () => {
    const mappingId = $("adaptMapping").value;
    if (!mappingId) {
      alert("Select Auto or a saved mapping.");
      return;
    }
    if (mappingId === "__auto__" && !$("adaptTemplate").value) {
      alert("Select a target template.");
      return;
    }
    const fd = new FormData();
    fd.append("mappingId", mappingId);
    fd.append("template", $("adaptTemplate").value || "");
    fd.append("recipe", $("adaptRecipe").value || "auto");
    const files = $("adaptSources").files;
    for (const f of files) fd.append("sources", f);
    $("adaptStatus").textContent = "Adapting…";
    try {
      const result = await api("/api/adapt/run", { method: "POST", body: fd });
      state.adapt.jobId = result.jobId;
      state.adapt.mappingId = result.mappingId;
      state.adapt.templatePath = result.templatePath || $("adaptTemplate").value;
      state.adapt.reviews = result.reviews || [];
      state.adapt.index = 0;
      state.adapt.approved = {};
      $("adaptStatus").textContent =
        `Adapted ${state.adapt.reviews.length} file(s). Review and adjust slot bindings below.`;
      $("adaptSources").value = "";
      renderAdaptReview();
    } catch (e) {
      $("adaptStatus").textContent = e.message;
      $("adaptReview").hidden = true;
    }
  });

  $("adaptApplyRest").addEventListener("click", async () => {
    if (!state.adapt.jobId) {
      alert("Run adapt first.");
      return;
    }
    const bindings = collectAdaptBindings();
    if (!bindings.length) {
      alert("This job has no editable bindings (saved path mappings cannot be rebound here).");
      return;
    }
    $("adaptBindStatus").textContent = "Rebinding…";
    try {
      const result = await api("/api/adapt/rebind", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ jobId: state.adapt.jobId, bindings }),
      });
      state.adapt.reviews = result.reviews || [];
      $("adaptBindStatus").textContent = "Applied bindings to all articles in this job.";
      renderAdaptReview();
    } catch (e) {
      $("adaptBindStatus").textContent = e.message;
    }
  });

  $("adaptSaveMapping").addEventListener("click", async () => {
    const id = ($("adaptSaveId").value || "").trim();
    if (!id) {
      alert("Enter a mapping id to save.");
      return;
    }
    const review = currentAdaptReview();
    const template = state.adapt.templatePath || $("adaptTemplate").value;
    if (!template) {
      alert("Select the target template this mapping should use.");
      return;
    }
    $("adaptBindStatus").textContent = "Saving…";
    try {
      const result = await api("/api/adapt/mappings", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          id,
          targetTemplate: template,
          sourceRecipe: review && review.recipeId ? review.recipeId : ($("adaptRecipe").value || "auto"),
          bindStrategy: review && review.bindStrategy ? review.bindStrategy : null,
          bindings: collectAdaptBindings().filter((b) => b.unit && b.unit !== "__empty__"),
        }),
      });
      $("adaptBindStatus").textContent = "Saved " + result.path;
      await loadAdaptMappings();
      $("adaptMapping").value = result.id;
      toggleAdaptAutoFields();
    } catch (e) {
      $("adaptBindStatus").textContent = e.message;
    }
  });

  $("adaptPrev").addEventListener("click", () => {
    if (state.adapt.index > 0) {
      state.adapt.index -= 1;
      renderAdaptReview();
    }
  });
  $("adaptNext").addEventListener("click", () => {
    if (state.adapt.index < state.adapt.reviews.length - 1) {
      state.adapt.index += 1;
      renderAdaptReview();
    }
  });
  $("adaptApprove").addEventListener("change", () => {
    const review = currentAdaptReview();
    if (!review) return;
    if ($("adaptApprove").checked) {
      state.adapt.approved[review.adaptedFile] = true;
    } else {
      delete state.adapt.approved[review.adaptedFile];
    }
  });

  $("adaptSendArticles").addEventListener("click", async () => {
    if (!state.selectedId) {
      alert("Select a blueprint first so articles land under input/articles/<id>/.");
      return;
    }
    if (!state.adapt.jobId) {
      alert("Run adapt first.");
      return;
    }
    const files = Object.keys(state.adapt.approved);
    if (!files.length) {
      alert("Approve at least one adapted file.");
      return;
    }
    $("adaptSendStatus").textContent = "Copying…";
    try {
      const result = await api("/api/adapt/send-to-articles", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          jobId: state.adapt.jobId,
          blueprintId: state.selectedId,
          files,
        }),
      });
      $("adaptSendStatus").textContent =
        `Sent ${result.articles.length} file(s) to articles. Build package when ready.`;
      await refreshReadiness();
    } catch (e) {
      $("adaptSendStatus").textContent = e.message;
    }
  });

  loadSettings().catch((e) => console.warn("Settings load failed:", e.message));
  loadAdaptMappings().catch(() => {});
  loadAdaptTemplates().catch(() => {});
  loadAdaptRecipes().catch(() => {});
  refreshList().catch((e) => alert(e.message));
})();
