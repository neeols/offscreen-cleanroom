/* Small enhancements to standard Javadoc pages. */
document.addEventListener("DOMContentLoaded", () => {
  const script = [...document.scripts].find((s) =>
    s.src.endsWith("offscreen.js"),
  );
  if (script) {
    const root = new URL(
      script.src.includes("/script-files/") ? "../" : ".",
      script.src,
    );
    const legacyViews = {
      "overview-tree.html": "tree",
      "index-all.html": "index",
      "search.html": "search",
    };
    for (const [page, view] of Object.entries(legacyViews)) {
      if (location.pathname === new URL(page, root).pathname) {
        const destination = new URL("browse.html", root);
        destination.search = location.search;
        destination.hash = location.hash || view;
        location.replace(destination.href);
        return;
      }
    }
    const navigation = document.querySelector("#navbar-top-firstrow");
    if (navigation) {
      const item = document.createElement("li");
      item.className = "av-reference-picker";
      const picker = document.createElement("select");
      picker.setAttribute("aria-label", "Kotlin and Scala documentation");
      picker.title = "Open a language reference in a new tab";
      [
        ["", "Language APIs ↗"],
        ["kotlin/index.html", "Dokka (Kotlin)"],
        ["scala/index.html", "Scaladoc (Scala 3)"],
      ].forEach(([value, text]) => {
        const option = document.createElement("option");
        option.value = value;
        option.textContent = text;
        if (!value) {
          option.disabled = true;
          option.hidden = true;
          option.selected = true;
        }
        picker.append(option);
      });
      picker.addEventListener("change", () => {
        const destination = picker.value;
        // This is a launcher, not persistent navigation state. Reset even if popups are blocked.
        picker.value = "";
        if (destination)
          window.open(
            new URL(destination, root).href,
            "_blank",
            "noopener,noreferrer",
          );
      });
      item.append(picker);
      navigation.append(item);
      addBrowsingTree(navigation, root);
    }
  }
  initializeBrowsePage();
  const guide = document.querySelector(".av-guide");
  if (!guide) return;
  // Keep one language preference for examples and their field tables. Each code toolbar
  // exposes the same selector, so readers need not scroll back to a page-level control.
  const languages = (guide.dataset.languages || "java groovy json").split(
    /\s+/,
  );
  let selectedLanguage = languages[0];
  guide.querySelector(".av-language")?.setAttribute("hidden", "");
  guide.querySelectorAll("img").forEach((img) => {
    img.loading = "lazy";
  });
  guide.querySelectorAll("a.av-video").forEach((link) => {
    const video = document.createElement("video");
    video.controls = true;
    video.preload = "none";
    video.src = link.href;
    video.poster = link.dataset.poster;
    link.replaceWith(video);
  });

  const choose = (value) => {
    if (!languages.includes(value)) value = languages[0];
    selectedLanguage = value;
    guide
      .querySelectorAll(".av-code-language:not(.av-recipe-language)")
      .forEach((select) => {
        select.value = [...select.options].some(
          (option) => option.value === value,
        )
          ? value
          : select.options[0].value;
      });
    guide.querySelectorAll("[data-language]").forEach((block) => {
      if (block.closest(".av-example-group")) return;
      block.hidden = !block.dataset.language.split(/\s+/).includes(value);
    });
    try {
      localStorage.setItem("offscreen-example-language", value);
    } catch {
      /* Storage is optional. */
    }
  };
  let preferred = "java";
  try {
    preferred = localStorage.getItem("offscreen-example-language") || preferred;
  } catch {
    /* File viewers may deny storage. */
  }
  choose(
    new URLSearchParams(window.location.search).get("language") || preferred,
  );
  guide.querySelectorAll("[data-choose-language]").forEach((button) => {
    button.addEventListener("click", () =>
      choose(button.dataset.chooseLanguage),
    );
  });
  const headings = [...guide.querySelectorAll("h2[id]")];
  if (headings.length > 1) {
    const nav = document.createElement("nav");
    nav.className = "av-toc";
    nav.setAttribute("aria-label", "On this page");
    const label = document.createElement("strong");
    label.textContent = "On this page";
    nav.append(label);
    const list = document.createElement("ul");
    headings.forEach((heading) => {
      const item = document.createElement("li");
      const link = document.createElement("a");
      link.href = "#" + heading.id;
      link.textContent = heading.textContent;
      item.append(link);
      list.append(item);
    });
    nav.append(list);
    (guide.querySelector(".av-lead") || guide.querySelector("h1")).after(nav);
  }
  // One stable toolbar per recipe: changing language must not hide the focused selector.
  guide.querySelectorAll(".av-example-group").forEach((group) => {
    const examples = [...group.querySelectorAll("pre[data-language]")];
    if (!examples.length) return;
    const wrap = document.createElement("div");
    wrap.className = "av-code-wrap";
    examples[0].before(wrap);
    const toolbar = document.createElement("div");
    toolbar.className = "av-code-toolbar";
    const selector = document.createElement("select");
    selector.className = "av-code-language av-recipe-language";
    selector.setAttribute(
      "aria-label",
      "Example language: " + group.querySelector("h2").textContent,
    );
    examples.forEach((pre) => {
      const option = document.createElement("option");
      option.value = pre.dataset.language;
      option.textContent =
        {
          java: "Java",
          kotlin: "Kotlin",
          groovy: "GroovyScript",
          scala: "Scala 3",
          zenscript: "CraftTweaker",
        }[option.value] || option.value;
      selector.append(option);
      pre.tabIndex = 0;
    });
    const chooseRecipe = (value) => {
      if (!examples.some((pre) => pre.dataset.language === value))
        value = examples[0].dataset.language;
      selector.value = value;
      examples.forEach((pre) => {
        pre.hidden = pre.dataset.language !== value;
      });
      const shared = group.querySelector(".av-shared-implementation");
      const bridge = group.querySelector(".av-zen-implementation");
      if (shared) {
        shared.hidden = value === "java";
        shared.open = true;
      }
      if (bridge) {
        bridge.hidden = value !== "zenscript";
        bridge.open = true;
      }
    };
    // Retain each recipe independently; changing one toolbar never resets its neighbors.
    const recipeKey =
      "offscreen-recipe-language:" +
      location.pathname +
      ":" +
      group.dataset.example;
    let recipeLanguage = selectedLanguage;
    try {
      recipeLanguage = localStorage.getItem(recipeKey) || recipeLanguage;
    } catch {
      /* Storage is optional. */
    }
    chooseRecipe(
      new URLSearchParams(location.search).get("language") || recipeLanguage,
    );
    selector.addEventListener("change", () => {
      chooseRecipe(selector.value);
      try {
        localStorage.setItem(recipeKey, selector.value);
      } catch {
        /* Storage is optional. */
      }
    });
    const copy = document.createElement("button");
    copy.type = "button";
    copy.className = "av-copy";
    copy.textContent = "Copy";
    copy.setAttribute("aria-label", "Copy selected language example");
    copy.setAttribute("aria-live", "polite");
    copy.addEventListener("click", async () => {
      const pre = examples.find((block) => !block.hidden);
      if (!pre) return;
      try {
        await navigator.clipboard.writeText(pre.textContent);
        copy.textContent = "Copied";
      } catch {
        const range = document.createRange();
        range.selectNodeContents(pre);
        const selection = window.getSelection();
        selection.removeAllRanges();
        selection.addRange(range);
        copy.textContent = "Selected";
      }
      window.setTimeout(() => {
        copy.textContent = "Copy";
      }, 2000);
    });
    group.querySelectorAll(".av-copy-implementation").forEach((button) => {
      button.addEventListener("click", async () => {
        const code = button.parentElement.querySelector("pre code");
        try {
          await navigator.clipboard.writeText(code.textContent);
          button.textContent = "Copied";
        } catch {
          const range = document.createRange();
          range.selectNodeContents(code);
          const selection = getSelection();
          selection.removeAllRanges();
          selection.addRange(range);
          button.textContent = "Selected";
        }
      });
    });
    toolbar.append(selector, copy);
    wrap.append(toolbar, ...examples);
  });
  guide.querySelectorAll("pre").forEach((pre) => {
    if (pre.closest(".av-example-group")) return;
    const wrap = document.createElement("div");
    wrap.className = "av-code-wrap";
    if (pre.dataset.language) {
      wrap.dataset.language = pre.dataset.language;
      wrap.hidden = pre.hidden;
    }
    pre.before(wrap);
    const button = document.createElement("button");
    button.className = "av-copy";
    button.type = "button";
    button.textContent = "Copy";
    button.setAttribute("aria-label", "Copy this code example");
    button.setAttribute("aria-live", "polite");
    const toolbar = document.createElement("div");
    toolbar.className = "av-code-toolbar";
    const selector = document.createElement("select");
    selector.className = "av-code-language";
    selector.setAttribute("aria-label", "Example language");
    const explicitLanguage = (
      pre.querySelector("code")?.className +
      " " +
      pre.className
    ).match(/\blang(?:uage)?-([\w-]+)/)?.[1];
    const declaredLanguage = pre.closest("[data-language]")?.dataset.language;
    // A language menu only offers alternatives supplied by the authored example.
    const available = explicitLanguage
      ? [explicitLanguage]
      : declaredLanguage
        ? languages
        : ["text"];
    available.forEach((value) => {
      const option = document.createElement("option");
      option.value = value;
      option.textContent =
        {
          java: "Java",
          groovy: "Groovy",
          json: "JSON",
          kotlin: "Kotlin",
          scala: "Scala 3",
          zenscript: "ZenScript",
          powershell: "PowerShell",
          config: "Configuration",
          command: "Command",
          text: "Text",
        }[value] || value;
      selector.append(option);
    });
    selector.value = available.includes(selectedLanguage)
      ? selectedLanguage
      : available[0];
    if (available.length > 1)
      selector.addEventListener("change", () => choose(selector.value));
    toolbar.append(selector, button);
    wrap.append(toolbar, pre);
    button.addEventListener("click", async () => {
      try {
        await navigator.clipboard.writeText(pre.textContent);
        button.textContent = "Copied";
      } catch {
        const range = document.createRange();
        range.selectNodeContents(pre);
        const selection = window.getSelection();
        selection.removeAllRanges();
        selection.addRange(range);
        button.textContent = "Selected";
      }
      window.setTimeout(() => {
        button.textContent = "Copy";
      }, 2000);
    });
  });
});

/** Switches between the JDK-generated hierarchy, alphabetical index and search without separate pages. */
function initializeBrowsePage() {
  const tabs = [...document.querySelectorAll("[data-av-browse]")];
  if (!tabs.length) return;
  const choose = () => {
    const hash = location.hash.substring(1);
    const view = hash.startsWith("I:")
      ? "index"
      : ["tree", "index", "search"].includes(hash)
        ? hash
        : new URLSearchParams(location.search).has("q")
          ? "search"
          : "tree";
    tabs.forEach((tab) => {
      const selected = tab.dataset.avBrowse === view;
      tab.setAttribute("aria-selected", String(selected));
      tab.tabIndex = selected ? 0 : -1;
      document.getElementById(tab.getAttribute("aria-controls")).hidden =
        !selected;
    });
    if (hash.startsWith("I:"))
      requestAnimationFrame(() =>
        document.getElementById(hash)?.scrollIntoView(),
      );
  };
  const activate = (tab) => {
    location.hash = tab.dataset.avBrowse;
    choose();
  };
  tabs.forEach((tab, index) => {
    tab.addEventListener("click", () => activate(tab));
    tab.addEventListener("keydown", (event) => {
      const targets = {
        ArrowLeft: (index + tabs.length - 1) % tabs.length,
        ArrowRight: (index + 1) % tabs.length,
        Home: 0,
        End: tabs.length - 1,
      };
      if (!(event.key in targets)) return;
      event.preventDefault();
      event.stopPropagation();
      const next = tabs[targets[event.key]];
      activate(next);
      next.focus();
    });
  });
  // The native search keyboard shortcuts must reveal their input even when another view is selected.
  document.addEventListener(
    "keydown",
    (event) => {
      if (
        event.key !== "/" ||
        event.ctrlKey ||
        event.altKey ||
        event.metaKey ||
        /INPUT|TEXTAREA|SELECT/.test(event.target.tagName) ||
        event.target.isContentEditable
      )
        return;
      event.preventDefault();
      activate(tabs.find((tab) => tab.dataset.avBrowse === "search"));
      document.getElementById("page-search-input").focus();
    },
    true,
  );
  window.addEventListener("hashchange", choose);
  choose();
}

/** Adds a lazy, keyboard-accessible browsing tree without replacing the JDK doclet navigation. */
function addBrowsingTree(navigation, root) {
  const item = document.createElement("li");
  item.className = "av-browse-item";
  const toggle = document.createElement("button");
  toggle.type = "button";
  toggle.className = "av-browse-toggle";
  toggle.textContent = "☰";
  toggle.title = "Browse documentation";
  toggle.setAttribute("aria-label", "Browse documentation");
  toggle.setAttribute("aria-controls", "av-browsing-tree");
  toggle.setAttribute("aria-expanded", "false");
  item.append(toggle);
  navigation.prepend(item);

  const sidebar = document.createElement("aside");
  sidebar.id = "av-browsing-tree";
  sidebar.setAttribute("aria-label", "Documentation browsing tree");
  sidebar.hidden = true;
  const heading = document.createElement("strong");
  heading.textContent = "Documentation";
  const content = document.createElement("div");
  content.className = "av-tree-content";
  sidebar.append(heading, content);
  const handle = document.createElement("div");
  handle.className = "av-sidebar-resize";
  handle.tabIndex = 0;
  handle.setAttribute("role", "separator");
  handle.setAttribute("aria-label", "Resize documentation sidebar");
  handle.setAttribute("aria-orientation", "vertical");
  handle.title = "Drag to resize; arrow keys also adjust width";
  sidebar.append(handle);
  document.body.append(sidebar);
  const read = (key, fallback) => {
    try {
      return localStorage.getItem(key) ?? fallback;
    } catch {
      return fallback;
    }
  };
  const save = (key, value) => {
    try {
      localStorage.setItem(key, String(value));
    } catch {
      /* Storage is optional. */
    }
  };
  let width = Number(read("offscreen-sidebar-width", 280));
  const limits = () => [
    Math.min(200, window.innerWidth * 0.4),
    Math.min(600, window.innerWidth * 0.7),
  ];
  const resize = (value) => {
    const [min, max] = limits();
    width = Math.round(
      Math.max(min, Math.min(max, Number.isFinite(value) ? value : 280)),
    );
    document.documentElement.style.setProperty(
      "--av-sidebar-width",
      width + "px",
    );
    handle.setAttribute("aria-valuemin", String(Math.round(min)));
    handle.setAttribute("aria-valuemax", String(Math.round(max)));
    handle.setAttribute("aria-valuenow", String(width));
  };
  const position = () => {
    const header = document.querySelector("body > header");
    const bottom = header?.getBoundingClientRect().bottom || 80;
    sidebar.style.top = Math.max(0, bottom) + "px";
    resize(width);
  };
  resize(width);
  position();
  let loaded = false;
  const load = async () => {
    if (loaded) return;
    loaded = true;
    content.textContent = "Loading…";
    try {
      const response = await fetch(new URL("docs-navigation.json", root));
      if (!response.ok) throw new Error("Navigation index unavailable");
      const entries = await response.json();
      const list = (nodes) => {
        const ul = document.createElement("ul");
        nodes.forEach((node) => {
          const li = document.createElement("li");
          if (node.children) {
            const folder = document.createElement("details");
            const summary = document.createElement("summary");
            summary.textContent = node.label;
            folder.append(summary, list(node.children));
            li.append(folder);
          } else {
            const link = document.createElement("a");
            link.textContent = node.label;
            link.href = new URL(node.href, root);
            if (new URL(link.href).pathname === location.pathname)
              link.setAttribute("aria-current", "page");
            li.append(link);
          }
          ul.append(li);
        });
        return ul;
      };
      content.replaceChildren(list(entries));
    } catch {
      loaded = false;
      content.textContent =
        "The tree could not load. Reopen it to retry, or use Overview.";
    }
  };
  const show = (open) => {
    sidebar.hidden = !open;
    toggle.setAttribute("aria-expanded", String(open));
    document.body.classList.toggle("av-sidebar-open", open);
    if (open) {
      position();
      load();
    }
  };
  toggle.addEventListener("click", () => show(sidebar.hidden));
  sidebar.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
      show(false);
      toggle.focus();
    }
  });
  handle.addEventListener("pointerdown", (event) => {
    if (event.button !== 0) return;
    event.preventDefault();
    handle.setPointerCapture(event.pointerId);
    document.body.classList.add("av-sidebar-resizing");
  });
  handle.addEventListener("pointermove", (event) => {
    if (handle.hasPointerCapture(event.pointerId)) resize(event.clientX);
  });
  const finish = () => {
    document.body.classList.remove("av-sidebar-resizing");
    save("offscreen-sidebar-width", width);
  };
  handle.addEventListener("pointerup", (event) => {
    if (handle.hasPointerCapture(event.pointerId))
      handle.releasePointerCapture(event.pointerId);
    finish();
  });
  handle.addEventListener("lostpointercapture", finish);
  handle.addEventListener("keydown", (event) => {
    const [min, max] = limits();
    const step = event.shiftKey ? 48 : 16;
    const values = {
      ArrowLeft: width - step,
      ArrowRight: width + step,
      Home: min,
      End: max,
    };
    if (event.key in values) {
      event.preventDefault();
      resize(values[event.key]);
      save("offscreen-sidebar-width", width);
    }
  });
  window.addEventListener("resize", position);
  const header = document.querySelector("body > header");
  if (window.ResizeObserver && header)
    new ResizeObserver(position).observe(header);
  // Persist only the chosen width. Each page starts with a closed tree and collapsed folders.
}
