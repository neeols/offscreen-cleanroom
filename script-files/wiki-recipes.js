/* Shared enhancements for authored guide recipes; no game runtime is involved. */
(() => {
  "use strict";
  const script = [...document.scripts].find((s) =>
    new URL(s.src || location.href).pathname.endsWith("/wiki-recipes.js"),
  );
  if (!script) return;
  const root = new URL("../", script.src);
  const url = (path) => new URL(path, root).href;
  const json = async (path) => {
    const response = await fetch(url(path));
    if (!response.ok) throw Error(path);
    return response.json();
  };
  const ready = (fn) =>
    document.readyState === "loading"
      ? document.addEventListener("DOMContentLoaded", fn)
      : fn();
  ready(async () => {
    const guide = document.querySelector(".av-guide");
    if (!guide) return;
    installInlineWrapping(guide);
    const page = decodeURIComponent(
      location.pathname.slice(new URL(root).pathname.length),
    );
    const [recipes, api, previews] = await Promise.all([
      json("recipe-tools/" + page.replaceAll("/", "__") + ".json").catch(
        () => [],
      ),
      json("recipe-tools/api.json").catch(() => null),
      json("recipe-tools/previews.json").catch(() => null),
    ]);
    const byId = new Map(recipes.map((recipe) => [recipe.id, recipe]));
    const staticPre = new WeakMap();
    const allPre = [...guide.querySelectorAll("pre")];
    for (const recipe of recipes)
      if (recipe.preIndex != null && allPre[recipe.preIndex])
        allPre[recipe.preIndex].dataset.wikiRecipe = recipe.id;
    for (const pre of allPre) {
      if (pre.closest(".av-code-wrap")) continue;
      const wrap = pre.closest(".wiki-download-block") || pre.parentElement;
      let toolbar = wrap.querySelector(":scope > .av-code-toolbar");
      if (!toolbar) {
        toolbar = document.createElement("div");
        toolbar.className = "av-code-toolbar";
        pre.before(toolbar);
      }
      let button =
        toolbar.querySelector(".av-copy:not(.wiki-download)") ||
        pre
          .closest("details")
          ?.querySelector(":scope > .av-copy-implementation");
      if (!button) {
        button = document.createElement("button");
        button.type = "button";
        button.textContent = "Copy";
        button.setAttribute("aria-label", "Copy this code example");
      }
      button.classList.add("av-copy");
      toolbar.append(button);
      staticPre.set(toolbar, pre);
    }
    const original = new WeakMap();
    guide
      .querySelectorAll("pre")
      .forEach((pre) => original.set(pre, pre.textContent));
    const recipeFor = (pre) =>
      byId.get(
        pre.dataset.wikiRecipe ||
          pre.closest("[data-example]")?.dataset.example,
      );
    function selected(toolbar) {
      if (staticPre.has(toolbar)) return staticPre.get(toolbar);
      const group = toolbar.closest(".av-example-group");
      return group
        ? [...group.querySelectorAll("pre[data-language]")].find(
            (pre) => !pre.hidden,
          )
        : toolbar.closest(".av-code-wrap")?.querySelector("pre");
    }
    function language(pre) {
      return (
        pre.querySelector("code")?.className.match(/language-([\w-]+)/)?.[1] ||
        pre.dataset.language ||
        pre.closest("[data-language]")?.dataset.language?.split(/\s+/)[0] ||
        "text"
      );
    }
    const copyStates = new WeakMap(),
      menus = [];
    function closeCopyMenu(state, focus = false) {
      if (state.menu.hidePopover) {
        if (state.menu.matches(":popover-open")) state.menu.hidePopover();
      } else state.menu.hidden = true;
      state.button.setAttribute("aria-expanded", "false");
      if (focus) state.button.focus();
    }
    guide.querySelectorAll(".av-code-toolbar").forEach((toolbar) => {
      const button = toolbar.querySelector(".av-copy:not(.wiki-download)");
      if (!button) return;
      const menu = document.createElement("div");
      menu.className = "wiki-copy-menu";
      menu.id = "wiki-copy-menu-" + menus.length;
      menu.setAttribute("role", "menu");
      menu.setAttribute("aria-label", "Copy code");
      menu.setAttribute("popover", "auto");
      if (!menu.showPopover) menu.hidden = true;
      const state = { toolbar, button, menu, label: button.textContent };
      for (const [value, label] of [
        ["full", "Full example"],
        ["imports", "Imports only"],
        ["body", "Method body"],
      ]) {
        const item = document.createElement("button");
        item.type = "button";
        item.textContent = label;
        item.dataset.copyMode = value;
        item.setAttribute("role", "menuitem");
        menu.append(item);
        copyStates.set(item, state);
      }
      document.body.append(menu);
      menus.push(state);
      copyStates.set(button, state);
      button.setAttribute("aria-haspopup", "menu");
      button.setAttribute("aria-expanded", "false");
      button.setAttribute("aria-controls", menu.id);
      state.update = () => {
        const pre = selected(toolbar),
          variant = pre && recipeFor(pre)?.variants[language(pre)];
        menu.children[1].disabled = !variant?.imports;
        menu.children[2].disabled = variant?.body == null;
      };
      toolbar.addEventListener("change", () => {
        state.update();
        closeCopyMenu(state);
      });
      menu.addEventListener("toggle", () =>
        button.setAttribute(
          "aria-expanded",
          String(menu.matches(":popover-open")),
        ),
      );
      menu.addEventListener("keydown", (event) => {
        const items = [...menu.children].filter((item) => !item.disabled),
          index = items.indexOf(document.activeElement);
        let next;
        if (event.key === "ArrowDown") next = items[(index + 1) % items.length];
        if (event.key === "ArrowUp")
          next = items[(index - 1 + items.length) % items.length];
        if (event.key === "Home") next = items[0];
        if (event.key === "End") next = items.at(-1);
        if (event.key === "Escape") {
          event.preventDefault();
          closeCopyMenu(state, true);
        }
        if (event.key === "Tab") closeCopyMenu(state);
        if (next) {
          event.preventDefault();
          next.focus();
        }
      });
      state.update();
    });
    // Handle copying before the original toolbar's full-source listener.
    document.addEventListener(
      "click",
      async (event) => {
        const target = event.target.closest("button"),
          state = copyStates.get(target);
        if (!state) {
          for (const item of menus) closeCopyMenu(item);
          return;
        }
        event.preventDefault();
        event.stopImmediatePropagation();
        state.update();
        if (target === state.button) {
          clearTimeout(state.feedbackTimer);
          state.button.textContent = state.label;
          const wasOpen = state.button.getAttribute("aria-expanded") === "true";
          for (const item of menus) closeCopyMenu(item);
          if (wasOpen) return;
          const bounds = target.getBoundingClientRect();
          state.menu.style.left =
            Math.max(8, Math.min(bounds.right - 184, innerWidth - 192)) + "px";
          state.menu.style.top =
            Math.max(8, Math.min(bounds.bottom + 4, innerHeight - 140)) + "px";
          if (state.menu.showPopover) state.menu.showPopover();
          else state.menu.hidden = false;
          state.button.setAttribute("aria-expanded", "true");
          [...state.menu.children].find((item) => !item.disabled)?.focus();
          return;
        }
        if (target.disabled) return;
        const pre = selected(state.toolbar);
        if (!pre) return;
        const variant = recipeFor(pre)?.variants[language(pre)],
          mode = target.dataset.copyMode;
        const text =
          mode === "full"
            ? (variant?.full ?? original.get(pre))
            : variant?.[mode];
        if (text == null) return;
        closeCopyMenu(state, true);
        try {
          await navigator.clipboard.writeText(text);
          state.button.textContent = "Copied";
        } catch {
          let fallback = state.toolbar.parentElement.querySelector(
            ".wiki-copy-fallback",
          );
          if (!fallback) {
            fallback = document.createElement("textarea");
            fallback.className = "wiki-copy-fallback";
            fallback.readOnly = true;
            fallback.setAttribute(
              "aria-label",
              "Selected code. Press Control+C or Command+C to copy.",
            );
            state.toolbar.after(fallback);
          }
          fallback.value = text;
          fallback.focus();
          fallback.select();
          state.button.textContent = "Selected";
        }
        clearTimeout(state.feedbackTimer);
        state.feedbackTimer = setTimeout(() => {
          state.button.textContent = state.label;
        }, 1800);
      },
      true,
    );
    const related = [
      ...new Map(
        recipes.flatMap((r) => r.related).map((r) => [r.href, r]),
      ).values(),
    ].slice(0, 3);
    if (related.length) {
      const section = document.createElement("section");
      section.className = "wiki-related";
      const title = document.createElement("h2");
      title.id = "related-recipes";
      title.textContent = "Related recipes";
      const list = document.createElement("ul");
      for (const item of related) {
        const li = document.createElement("li"),
          a = document.createElement("a");
        a.href = url(item.href);
        a.textContent = item.label;
        li.append(a);
        list.append(li);
      }
      section.append(title, list);
      const pager = guide.querySelector(".av-pager");
      pager ? pager.before(section) : guide.append(section);
    }
    if (api) {
      const linked = new WeakSet();
      function linkCode(code) {
        if (
          linked.has(code) ||
          !code.classList.contains("av-syntax") ||
          code.closest("[hidden]")
        )
          return;
        linked.add(code);
        const source = code.textContent,
          imports = new Map(),
          wildcards = [];
        for (const match of source.matchAll(
          /^\s*import\s+(?:static\s+)?([\w.]+)(\.\*)?(?:\s+as\s+(\w+))?/gm,
        )) {
          if (match[2]) wildcards.push(match[1]);
          else {
            const qualified = api.aliases[match[1]] || match[1];
            imports.set(match[3] || match[1].split(".").at(-1), qualified);
          }
        }
        const shadows = new Set(
          [
            ...source.matchAll(/\b(?:class|interface|object|enum)\s+(\w+)/g),
          ].map((m) => m[1]),
        );
        const resolve = (name) => {
          if (shadows.has(name)) return null;
          if (imports.has(name) && api.types[imports.get(name)])
            return imports.get(name);
          const possibilities = (api.simple[name] || []).filter((qualified) =>
            wildcards.some((prefix) => qualified.startsWith(prefix + ".")),
          );
          return possibilities.length === 1
            ? possibilities[0]
            : api.simple[name]?.length === 1 && !imports.has(name)
              ? api.simple[name][0]
              : null;
        };
        const pre = code.closest("pre"),
          receivers = { ...(recipeFor(pre)?.receivers || {}) };
        for (const match of source.matchAll(
          /\b([A-Z]\w*(?:\.[A-Z]\w*)*)\s+(\w+)\s*(?=[,)=;])/g,
        )) {
          const type = resolve(match[1]);
          if (type) receivers[match[2]] = type;
        }
        for (const match of source.matchAll(
          /\b(\w+)\s*(?::|as)\s*([A-Z]\w*(?:\.[A-Z]\w*)*)/g,
        )) {
          const type = resolve(match[2]);
          if (type) receivers[match[1]] = type;
        }
        // Walk once, carrying offsets into unchanged source. Comments and string literals are never linked.
        const walker = document.createTreeWalker(code, NodeFilter.SHOW_TEXT),
          nodes = [];
        let offset = 0;
        while (walker.nextNode()) {
          nodes.push([walker.currentNode, offset]);
          offset += walker.currentNode.nodeValue.length;
        }
        for (const [node, start] of nodes) {
          if (node.parentElement.closest("a,.comment,.string,.char")) continue;
          const text = node.nodeValue;
          if (!/^[A-Za-z_]\w*$/.test(text)) continue;
          let target = null,
            isType = false;
          const type = resolve(text);
          if (
            type &&
            (node.parentElement.closest(".class-name") || /^[A-Z]/.test(text))
          ) {
            target = api.types[type];
            isType = true;
          } else if (node.parentElement.closest(".function")) {
            const receiver = source
              .slice(Math.max(0, start - 180), start)
              .match(/([A-Za-z_]\w*)\s*\.\s*$/)?.[1];
            const owner =
              receiver && (receivers[receiver] || resolve(receiver));
            const choices = owner && api.members[owner + "." + text];
            if (choices?.length === 1) target = choices[0];
          }
          if (!target) continue;
          const a = document.createElement("a");
          a.className = "wiki-api-link";
          a.href = url(target);
          a.title = "API reference: " + text;
          a.textContent = text;
          if (isType && !node.parentElement.closest(".class-name")) {
            const token = document.createElement("span");
            token.className = "token class-name";
            token.append(a);
            node.replaceWith(token);
          } else node.replaceWith(a);
        }
      }
      const scan = () => guide.querySelectorAll("pre > code").forEach(linkCode);
      scan();
      let queued = false;
      new MutationObserver(() => {
        if (!queued) {
          queued = true;
          queueMicrotask(() => {
            queued = false;
            scan();
          });
        }
      }).observe(guide, {
        subtree: true,
        attributes: true,
        attributeFilter: ["class", "hidden"],
        childList: true,
      });
    }
    installPreviews(guide, previews, url);
  });

  function installInlineWrapping(guide) {
    const units = [],
      measure = document.createElement("canvas").getContext("2d");
    if (!measure) return;
    for (const code of guide.querySelectorAll("code")) {
      if (code.closest("pre")) continue;
      const matches = [
        ...code.textContent.matchAll(/[A-Za-z_$][\w.$]*\([^()\n]*\)/g),
      ]
        .filter((match) => match[0].length <= 48)
        .reverse();
      function boundary(offset, start) {
        const walker = document.createTreeWalker(code, NodeFilter.SHOW_TEXT);
        let position = 0,
          last;
        while (walker.nextNode()) {
          const node = walker.currentNode,
            end = position + node.length;
          last = node;
          if (offset < end || (!start && offset === end))
            return { node, offset: offset - position };
          position = end;
        }
        return { node: last, offset: last.length };
      }
      for (const match of matches) {
        const from = boundary(match.index, true),
          to = boundary(match.index + match[0].length, false);
        const range = document.createRange();
        range.setStart(from.node, from.offset);
        range.setEnd(to.node, to.offset);
        if (from.offset === 0) {
          let node = from.node;
          while (
            node.parentNode !== code &&
            node === node.parentNode.firstChild
          )
            node = node.parentNode;
          range.setStartBefore(node);
        }
        if (to.offset === to.node.length) {
          let node = to.node;
          while (node.parentNode !== code && node === node.parentNode.lastChild)
            node = node.parentNode;
          range.setEndAfter(node);
        }
        const startAnchor = (
          range.startContainer.nodeType === Node.ELEMENT_NODE
            ? range.startContainer
            : range.startContainer.parentElement
        ).closest("a");
        if (startAnchor && !startAnchor.contains(range.endContainer)) continue;
        const span = document.createElement("span");
        span.className = "wiki-inline-unit";
        span.append(range.extractContents());
        range.insertNode(span);
        units.push(span);
      }
    }
    let pending = false;
    function update() {
      pending = false;
      for (const unit of units) {
        const cell = unit.closest("td,th,p,li,dd") || unit.parentElement;
        const style = getComputedStyle(unit),
          container = getComputedStyle(cell);
        measure.font = style.font;
        const available =
          cell.clientWidth -
          parseFloat(container.paddingLeft) -
          parseFloat(container.paddingRight);
        unit.classList.toggle(
          "wiki-inline-keep",
          measure.measureText(unit.textContent).width <= available - 2,
        );
      }
    }
    function schedule() {
      if (!pending) {
        pending = true;
        requestAnimationFrame(update);
      }
    }
    new ResizeObserver(schedule).observe(guide);
    window.addEventListener("resize", schedule);
    document.fonts?.ready.then(schedule);
    update();
  }
  function installPreviews(guide, records, url) {
    const reduced = matchMedia("(prefers-reduced-motion: reduce)");
    const cache = new Map();
    let memory = 0;
    const LIMIT = 48 * 1024 * 1024;
    async function bitmap(path, signal) {
      const key = url(path);
      if (cache.has(key)) {
        const entry = cache.get(key);
        cache.delete(key);
        cache.set(key, entry);
        return entry.bitmap;
      }
      const response = await fetch(key, { signal });
      if (!response.ok) throw Error("Missing preview frame");
      const image = await createImageBitmap(await response.blob());
      if (signal.aborted) {
        image.close();
        throw new DOMException("Aborted", "AbortError");
      }
      const bytes = image.width * image.height * 4;
      if (cache.has(key)) {
        image.close();
        return cache.get(key).bitmap;
      }
      while (memory + bytes > LIMIT && cache.size) {
        const [old, entry] = cache.entries().next().value;
        cache.delete(old);
        memory -= entry.bytes;
        entry.bitmap.close();
      }
      if (bytes > LIMIT) {
        image.close();
        throw Error("Preview frame exceeds memory limit");
      }
      cache.set(key, { bitmap: image, bytes });
      memory += bytes;
      return image;
    }
    for (const img of [...guide.querySelectorAll("img")]) {
      const path = decodeURIComponent(
        new URL(img.src).pathname.slice(new URL(url("")).pathname.length),
      );
      const record = records?.[path];
      if (!record) {
        if (records === null && path.toLowerCase().endsWith(".gif")) {
          const fallback = document.createElement("p"),
            link = document.createElement("a");
          fallback.textContent = "Preview unavailable. ";
          link.href = img.src;
          link.textContent = "Open original GIF";
          fallback.append(link);
          img.replaceWith(fallback);
        }
        continue;
      }
      const wrapper = document.createElement("div");
      wrapper.className = "wiki-preview";
      const viewport = document.createElement("div");
      viewport.className = "wiki-preview-viewport";
      const canvas = document.createElement("canvas");
      canvas.width = record.width;
      canvas.height = record.height;
      canvas.style.width = record.width + "px";
      canvas.style.height = "auto";
      canvas.setAttribute("role", "img");
      canvas.setAttribute("aria-label", img.alt || "Animated preview");
      const controls = document.createElement("div");
      controls.className = "wiki-preview-controls";
      const play = document.createElement("button"),
        replay = document.createElement("button"),
        full = document.createElement("button");
      for (const button of [play, replay, full]) button.type = "button";
      replay.textContent = "Replay";
      full.textContent = "Full size";
      const status = document.createElement("span");
      status.setAttribute("role", "status");
      status.className = "wiki-preview-status";
      const original = document.createElement("a");
      original.href = url(record.original);
      original.textContent = "Original GIF";
      controls.append(play, replay, full, original, status);
      viewport.append(canvas);
      wrapper.append(viewport, controls);
      img.replaceWith(wrapper);
      const context = canvas.getContext("2d");
      context.imageSmoothingEnabled = false;
      let visible = false,
        wanted = !reduced.matches,
        failed = false,
        index = 0,
        elapsed = 0,
        last = null,
        raf = 0,
        generation = 0,
        controller = new AbortController();
      const pending = new Map();
      const load = (i) => {
        if (!pending.has(i)) {
          const abort = new AbortController(),
            parent = controller.signal,
            cancel = () => abort.abort();
          parent.addEventListener("abort", cancel, { once: true });
          const request = bitmap(record.frames[i].src, abort.signal).finally(
            () => {
              parent.removeEventListener("abort", cancel);
              if (pending.get(i)?.request === request) pending.delete(i);
            },
          );
          pending.set(i, { request, abort });
          request.catch(() => {
            if (pending.get(i)?.request === request) pending.delete(i);
          });
        }
        return pending.get(i).request;
      };
      const active = () => wanted && visible && !document.hidden && !failed;
      function label() {
        play.textContent = wanted ? "Pause" : "Play";
        play.setAttribute(
          "aria-label",
          wanted ? "Pause preview" : "Play preview",
        );
        play.setAttribute("aria-pressed", String(wanted));
        wrapper.dataset.playing = String(active());
      }
      function stop() {
        cancelAnimationFrame(raf);
        raf = 0;
        last = null;
        generation++;
        controller.abort();
        controller = new AbortController();
        pending.clear();
        label();
      }
      function fail(error) {
        if (error.name === "AbortError") return;
        failed = true;
        wanted = false;
        stop();
        status.textContent = "Preview unavailable. Open the original GIF.";
      }
      async function paint(i, token) {
        try {
          const image = await load(i);
          if (token !== generation) return;
          context.clearRect(0, 0, canvas.width, canvas.height);
          context.drawImage(image, 0, 0);
          canvas.dataset.frame = String(i);
        } catch (error) {
          if (token === generation) fail(error);
        }
      }
      function tick(now) {
        if (!active()) {
          stop();
          return;
        }
        if (last !== null) elapsed += now - last;
        last = now;
        while (elapsed >= record.frames[index].ms) {
          elapsed -= record.frames[index].ms;
          index = (index + 1) % record.frames.length;
        }
        if (canvas.dataset.frame !== String(index))
          void paint(index, generation);
        for (let ahead = 1; ahead <= 2; ahead++) {
          const token = generation;
          load((index + ahead) % record.frames.length).catch((error) => {
            if (token === generation) fail(error);
          });
        }
        // Pending requests are limited to current + two frames; cached frames are bounded globally.
        for (const key of pending.keys())
          if (
            ![
              index,
              (index + 1) % record.frames.length,
              (index + 2) % record.frames.length,
            ].includes(key)
          ) {
            pending.get(key).abort.abort();
            pending.delete(key);
          }
        raf = requestAnimationFrame(tick);
        label();
      }
      function resume() {
        label();
        if (active() && !raf) {
          last = null;
          raf = requestAnimationFrame(tick);
        }
      }
      play.addEventListener("click", () => {
        wanted = !wanted;
        if (wanted) resume();
        else stop();
      });
      replay.addEventListener("click", () => {
        stop();
        index = 0;
        elapsed = 0;
        failed = false;
        wanted = true;
        status.textContent = "";
        void paint(0, generation);
        resume();
      });
      document.addEventListener("visibilitychange", () => {
        document.hidden ? stop() : resume();
      });
      reduced.addEventListener("change", (event) => {
        if (event.matches) {
          wanted = false;
          stop();
        }
      });
      const observer = new IntersectionObserver(
        (entries) => {
          visible = entries[0].isIntersecting;
          visible ? resume() : stop();
        },
        { threshold: 0 },
      );
      observer.observe(canvas);
      const marker = document.createComment("preview position");
      wrapper.before(marker);
      full.addEventListener("click", () => {
        if (typeof HTMLDialogElement === "undefined") {
          window.open(url(record.original), "_blank", "noopener,noreferrer");
          return;
        }
        const dialog = document.createElement("dialog");
        dialog.className = "wiki-preview-dialog";
        dialog.setAttribute("aria-label", "Full-size preview");
        const close = document.createElement("button");
        close.type = "button";
        close.textContent = "Close full-size preview";
        dialog.append(close, wrapper);
        document.body.append(dialog);
        close.addEventListener("click", () => dialog.close());
        dialog.addEventListener("close", () => {
          marker.after(wrapper);
          dialog.remove();
          full.focus();
        });
        dialog.showModal();
        close.focus();
      });
      label();
      // A static PNG poster does not decode the original animated image while offscreen.
      const poster = new Image();
      poster.onload = () => {
        if (canvas.dataset.frame === undefined) context.drawImage(poster, 0, 0);
      };
      poster.src = url(record.frames[0].src);
    }
    for (const video of guide.querySelectorAll("video")) {
      video.controls = true;
      const controls = document.createElement("div");
      controls.className = "wiki-preview-controls";
      const replay = document.createElement("button"),
        full = document.createElement("button");
      replay.type = full.type = "button";
      replay.textContent = "Replay";
      full.textContent = "Full size";
      controls.append(replay, full);
      video.after(controls);
      replay.addEventListener("click", () => {
        video.currentTime = 0;
        void video.play().catch(() => {});
      });
      full.addEventListener("click", () => {
        if (video.requestFullscreen)
          void video
            .requestFullscreen()
            .catch(() =>
              window.open(
                video.currentSrc || video.src,
                "_blank",
                "noopener,noreferrer",
              ),
            );
        else
          window.open(
            video.currentSrc || video.src,
            "_blank",
            "noopener,noreferrer",
          );
      });
      video.addEventListener("loadedmetadata", () => {
        video.style.width = video.videoWidth + "px";
        video.style.height = "auto";
        video.style.maxWidth = "100%";
        video.style.maxHeight = "420px";
      });
      let visible = false,
        wanted = video.autoplay,
        internalPause = false;
      video.addEventListener("play", () => {
        wanted = true;
      });
      video.addEventListener("pause", () => {
        if (internalPause) internalPause = false;
        else wanted = false;
      });
      const suspend = () => {
        if (!video.paused) {
          internalPause = true;
          video.pause();
        }
      };
      const resume = () => {
        if (visible && !document.hidden && wanted)
          void video.play().catch(() => {});
      };
      new IntersectionObserver((entries) => {
        visible = entries[0].isIntersecting;
        visible ? resume() : suspend();
      }).observe(video);
      document.addEventListener("visibilitychange", () => {
        document.hidden ? suspend() : resume();
      });
    }
  }
})();
