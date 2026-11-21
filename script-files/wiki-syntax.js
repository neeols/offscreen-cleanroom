/* Keep client-generated recipe variants as readable as authored examples. */
(() => {
  window.WikiReadableCode = (source, language) => {
    if (!['java', 'kotlin', 'groovy', 'scala', 'zenscript', 'json'].includes(language)) return source;
    const protectedText = new Uint8Array(source.length);
    const literals = /"""[\s\S]*?"""|'''[\s\S]*?'''|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|\/\/[^\n]*|\/\*[\s\S]*?\*\//g;
    for (const match of source.matchAll(literals)) protectedText.fill(1, match.index, match.index + match[0].length);
    const edits = new Map(), expanded = [], chainIndents = new Map();
    let parens = 0, brackets = 0, lineStart = 0;
    for (let i = 0; i < source.length; i++) {
      const char = source[i];
      if (char === '\n') lineStart = i + 1;
      if (protectedText[i]) continue;
      if (char === '(') parens++;
      if (char === ')') parens--;
      if (char === '[') brackets++;
      if (char === ']') brackets--;
      let lineEnd = source.indexOf('\n', i);
      if (lineEnd < 0) lineEnd = source.length;
      const line = source.slice(lineStart, lineEnd), indent = line.match(/^[ \t]*/)[0];
      const last = expanded.at(-1), activeIndent = last != null ? last + '    ' : indent;
      let after = i + 1;
      while (after < lineEnd && /[ \t]/.test(source[after])) after++;
      if (char === ';' && language !== 'scala' && parens === 0 && brackets === 0 && after < lineEnd && !source.startsWith('//', after) && source[after] !== '}') edits.set(i + 1, [after, '\n' + activeIndent]);
      if (char === ';' && parens === 0) chainIndents.clear();
      if (char === '{') {
        const expand = language !== 'scala' && after < lineEnd && (line.includes(';') || line.length > 88);
        expanded.push(expand ? activeIndent : null);
        if (expand) edits.set(i + 1, [after, '\n' + activeIndent + '    ']);
      } else if (char === '}' && expanded.length) {
        const blockIndent = expanded.pop();
        if (blockIndent != null && source.slice(lineStart, i).trim()) {
          let start = i;
          while (start > lineStart && /[ \t]/.test(source[start - 1])) start--;
          edits.set(start, [i, '\n' + blockIndent]);
        }
      }
      if (char === '.' && /^[A-Za-z_$][\w$]*\s*\(/.test(source.slice(i + 1))) {
        let before = i - 1;
        while (before >= lineStart && /[ \t]/.test(source[before])) before--;
        if (before >= lineStart && ')}]'.includes(source[before]) && line.length > 88) {
          if (!chainIndents.has(parens)) chainIndents.set(parens, line.trimStart().startsWith('.') ? indent : indent + '    ');
          edits.set(before + 1, [i, '\n' + chainIndents.get(parens)]);
        } else if (!source.slice(lineStart, i).trim()) {
          if (!chainIndents.has(parens)) chainIndents.set(parens, indent);
          if (chainIndents.get(parens) !== indent) edits.set(lineStart, [i, chainIndents.get(parens)]);
        }
      }
      if (char === ',' && parens > 0 && line.length > 88 && after < lineEnd && !source.startsWith('//', after)) edits.set(i + 1, [after, '\n' + indent + '    ']);
    }
    let result = '';
    for (let i = 0; i < source.length;) {
      if (edits.has(i)) {
        const [end, replacement] = edits.get(i);
        result += replacement;
        if (end === i) result += source[i++];
        else i = end;
      } else result += source[i++];
    }
    return result;
  };
})();

/* Shared, offline syntax coloring for the Wiki's copyable examples. */
(() => {
  "use strict";
  const prism = window.WikiSyntaxPrism;
  if (!prism) return;

  // Include Scala 3's newer control words and ordinary calls in the shared palette.
  prism.languages.scala.keyword = [
    /\b(?:end|export|then)\b/,
    prism.languages.scala.keyword,
  ];
  prism.languages.insertBefore("scala", "operator", {
    function: /\b[a-z_]\w*(?=\s*\()/,
    "class-name": /\b[A-Z]\w*\b/,
  });

  // ZenScript 1.12 has its own keywords, bracket expressions and loader directives.
  // It is not Java: in particular, `as`, `val` and `function` are language syntax.
  prism.languages.zenscript = {
    directive: {
      pattern:
        /^[\t ]*#(?:debug|ignoreBracketErrors|loader|modloaded|norun|priority|reloadable)\b[^\r\n]*/m,
      alias: "annotation",
    },
    comment: [
      { pattern: /\/\*[\s\S]*?(?:\*\/|$)/, greedy: true },
      { pattern: /\/\/[^\r\n]*|#[^\r\n]*/, greedy: true },
    ],
    string: { pattern: /(["'])(?:\\[\s\S]|(?!\1)[^\\\r\n])*\1/, greedy: true },
    bracket: {
      pattern: /<[a-zA-Z_][\w.-]*:[^<>\r\n]+>/,
      alias: "symbol",
      inside: { punctuation: /[<>:]/ },
    },
    keyword:
      /\b(?:as|break|continue|else|expand|for|function|global|if|import|in|instanceof|null|return|static|to|val|var|while|zenClass|zenConstructor|zenGetter|zenSetter)\b/,
    builtin:
      /\b(?:any|bool|boolean|byte|double|float|int|long|short|string|void)\b/,
    boolean: /\b(?:true|false)\b/,
    number: /\b(?:0[xX][\da-fA-F]+|\d+(?:\.\d*)?(?:[eE][+-]?\d+)?)[fFdDlL]?\b/,
    function: /\b[a-zA-Z_]\w*(?=\s*\()/,
    "class-name": /\b[A-Z][\w]*\b/,
    operator: /\.{2}|[-+*/%=!<>]=?|&&?|\|\|?|\^|~|\?|:/,
    punctuation: /[{}\[\];(),.]/,
  };

  // Chat and console commands share flags, placeholders, quoted values and arrays.
  prism.languages.command = {
    comment: { pattern: /^\s*#[^\r\n]*/m, greedy: true },
    string: { pattern: /(["'])(?:\\[\s\S]|(?!\1)[^\\\r\n])*\1/, greedy: true },
    placeholder: { pattern: /<[^<>\r\n]+>/, alias: "variable" },
    option: {
      pattern: /(^|[\s\[])--?[a-zA-Z][\w-]*/,
      lookbehind: true,
      alias: "keyword",
    },
    command: {
      pattern:
        /^[\t ]*\/?[a-zA-Z][\w-]*(?:[\t ]+(?:cas|export|cache|help|configure|search|names|preview|import-list|import|status|cancel|open|clear))*/m,
      alias: "function",
    },
    number: /\b\d+(?:\.\d+)?\b/,
    punctuation: /[\[\]{},|]/,
  };

  const aliases = {
    crafttweaker: "zenscript",
    zs: "zenscript",
    groovyscript: "groovy",
    scala3: "scala",
    config: "properties",
  };

  // Build text nodes, rather than parsing highlighted HTML. This preserves tabs,
  // nonbreaking spaces, Unicode and literal markup exactly for the Copy button.
  function appendToken(parent, token) {
    if (typeof token === "string") {
      parent.append(document.createTextNode(token));
    } else if (Array.isArray(token)) {
      token.forEach((child) => appendToken(parent, child));
    } else {
      const span = document.createElement("span");
      span.classList.add("token", token.type);
      const names = token.alias ? [].concat(token.alias) : [];
      names.forEach((name) => span.classList.add(name));
      appendToken(span, token.content);
      parent.append(span);
    }
  }

  function highlightExamples() {
    document
      .querySelectorAll("pre > code, table code, p code")
      .forEach((code) => {
        if (code.classList.contains("av-syntax") || code.closest("[hidden]"))
          return;
        // Native references keep their own highlighting, and API hyperlinks remain usable.
        if (code.querySelector(".token, .hljs-keyword")) return;
        const explicit = code.className.match(/\blang(?:uage)?-([\w-]+)/i)?.[1];
        const declared = code
          .closest("[data-language]")
          ?.dataset.language.split(/\s+/)[0];
        const language = (
          explicit ||
          declared ||
          (code.closest("pre") ? "text" : "java")
        ).toLowerCase();
        const grammar = prism.languages[aliases[language] || language];
        if (
          !grammar ||
          ["text", "plain", "plaintext", "none"].includes(language)
        )
          return;
        const inline = !code.closest("pre");
        if (!inline && !code.parentElement.dataset.wikiReadable) {
          code.textContent = window.WikiReadableCode(code.textContent, aliases[language] || language);
          code.parentElement.dataset.wikiReadable = 'true';
        }
        const activeGrammar =
          inline && ["java", "kotlin", "scala", "groovy"].includes(language)
            ? {
                ...grammar,
                "class-name": [].concat(
                  grammar["class-name"] || [],
                  /\b[A-Z]\w*\b/,
                ),
              }
            : grammar;
        // Color only text nodes. Existing links, word-break hints and their targets stay intact.
        // Link labels are isolated from adjacent punctuation by the HTML.
        const linkedTokens = new Set();
        if (code.closest("table")) {
          for (const link of code.querySelectorAll("a[href]")) {
            const label = link.textContent.trim();
            if (!/^[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)*$/.test(label)) continue;
            const type = /^[A-Z][A-Z0-9_]+$/.test(label)
              ? "constant"
              : /^[A-Z]/.test(label)
                ? "class-name"
                : "function";
            link.classList.add("token", type);
            linkedTokens.add(link);
          }
        }
        const walker = document.createTreeWalker(code, NodeFilter.SHOW_TEXT);
        const nodes = [];
        while (walker.nextNode()) nodes.push(walker.currentNode);
        nodes.forEach((node) => {
          if (linkedTokens.has(node.parentElement.closest("a"))) return;
          const fragment = document.createDocumentFragment();
          appendToken(fragment, prism.tokenize(node.nodeValue, activeGrammar));
          node.replaceWith(fragment);
        });
        code.classList.add("av-syntax");
        code.dataset.syntaxLanguage = language;
        if (!inline) code.parentElement.classList.add("av-syntax-block");
      });
  }

  document.addEventListener("wiki-examples-changed", highlightExamples);
  if (document.readyState === "loading")
    document.addEventListener("DOMContentLoaded", highlightExamples, {
      once: true,
    });
  else highlightExamples();
})();

/* Download the displayed source without changing its whitespace or highlighting. */
(() => {
  "use strict";
  const extensions = {
    java: "java",
    kotlin: "kt",
    kt: "kt",
    groovy: "groovy",
    groovyscript: "groovy",
    scala: "scala",
    scala3: "scala",
    zenscript: "zs",
    crafttweaker: "zs",
    zs: "zs",
    json: "json",
    json5: "json5",
    javascript: "js",
    js: "js",
    typescript: "ts",
    python: "py",
    powershell: "ps1",
    bash: "sh",
    shell: "sh",
    sh: "sh",
    xml: "xml",
    html: "html",
    css: "css",
    yaml: "yaml",
    yml: "yml",
    properties: "properties",
    config: "cfg",
    toml: "toml",
    text: "txt",
    plaintext: "txt",
  };
  function language(pre) {
    const code = pre.querySelector("code") || pre;
    const explicit =
      pre.dataset.language ||
      (code.className + " " + pre.className).match(
        /\b(?:language|lang)-([\w-]+)/,
      )?.[1] ||
      code.dataset.syntaxLanguage ||
      pre.closest("[data-language]")?.dataset.language;
    if (explicit) return explicit.toLowerCase().split(/\s+/)[0];
    if (location.pathname.includes("/kotlin/")) return "kotlin";
    if (location.pathname.includes("/scala/")) return "scala";
    const text = code.textContent.trim();
    if (/^[{[]/.test(text)) {
      try {
        JSON.parse(text);
        return "json";
      } catch {}
    }
    if (
      /^(?:PS>|\$ |\.\/gradlew|gradlew|java -|python |git |npm |curl )/m.test(
        text,
      )
    )
      return "shell";
    if (/\b(?:public |import |class |void |new )/.test(text)) return "java";
    return "text";
  }
  function filename(pre) {
    const code = pre.querySelector("code") || pre;
    const supplied = pre.dataset.filename || code.dataset.filename;
    if (supplied) return supplied.replace(/[\\/:*?"<>|\x00-\x1f]/g, "-");
    const lang = language(pre),
      ext = extensions[lang] || "txt";
    const source = code.textContent.replace(/\/\*[\s\S]*?\*\/|\/\/[^\n]*/g, "");
    const declaration =
      [
        "java",
        "kotlin",
        "kt",
        "scala",
        "scala3",
        "groovy",
        "groovyscript",
      ].includes(lang) &&
      source.match(
        /\b(?:class|interface|enum|record|object|trait)\s+([A-Za-z_$][\w$]*)/,
      );
    if (declaration) return declaration[1] + "." + ext;
    const group = pre.closest(".av-example-group");
    let heading = group?.querySelector("h2,h3")?.textContent;
    if (!heading) {
      for (const element of document.querySelectorAll("h1,h2,h3,h4")) {
        if (
          element.compareDocumentPosition(pre) &
          Node.DOCUMENT_POSITION_FOLLOWING
        )
          heading = element.textContent;
      }
    }
    const title =
      (heading || document.title || "example")
        .normalize("NFKD")
        .replace(/[^\w -]/g, "")
        .trim()
        .replace(/[ _]+/g, "-")
        .toLowerCase()
        .slice(0, 80) || "example";
    return (
      title +
      "-" +
      (Array.from(document.querySelectorAll("pre")).indexOf(pre) + 1) +
      "." +
      ext
    );
  }
  function install() {
    // Language toolbars may move their pre elements after another script runs.
    // Discard empty provisional rows, never leave one row per hidden language.
    document.querySelectorAll(".wiki-download-block").forEach((wrap) => {
      if (!wrap.querySelector("pre")) wrap.remove();
    });
    document.querySelectorAll(".av-code-toolbar").forEach((toolbar) => {
      const buttons = Array.from(toolbar.querySelectorAll(".wiki-download"));
      buttons.slice(1).forEach((button) => button.remove());
      if (buttons[0]) toolbar.prepend(buttons[0]);
    });
    document.querySelectorAll("pre").forEach((pre) => {
      if (pre.dataset.downloadReady) return;
      pre.dataset.downloadReady = "true";
      let wrap = pre.parentElement;
      let toolbar = wrap.classList.contains("av-code-wrap")
        ? wrap.querySelector(":scope > .av-code-toolbar")
        : null;
      if (!toolbar) {
        wrap = document.createElement("div");
        wrap.className = "wiki-download-block";
        pre.before(wrap);
        wrap.append(pre);
        toolbar = document.createElement("div");
        toolbar.className = "av-code-toolbar wiki-download-toolbar";
        wrap.prepend(toolbar);
      }
      if (toolbar.querySelector(".wiki-download")) return;
      const button = document.createElement("button");
      button.type = "button";
      button.className = "av-copy wiki-download";
      button.title = "Download code";
      const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
      svg.setAttribute("viewBox", "0 0 24 24");
      svg.setAttribute("width", "16");
      svg.setAttribute("height", "16");
      svg.setAttribute("aria-hidden", "true");
      const path = document.createElementNS(svg.namespaceURI, "path");
      path.setAttribute("d", "M12 3v12m-4-4 4 4 4-4M5 17v4h14v-4");
      path.setAttribute("fill", "none");
      path.setAttribute("stroke", "currentColor");
      path.setAttribute("stroke-width", "1.8");
      path.setAttribute("stroke-linecap", "round");
      path.setAttribute("stroke-linejoin", "round");
      svg.append(path);
      button.append(svg);
      button.setAttribute("aria-label", "Download this code example");
      button.addEventListener("click", () => {
        const selected =
          Array.from(wrap.querySelectorAll(":scope > pre")).find(
            (block) => !block.hidden,
          ) || pre;
        const content = (selected.querySelector("code") || selected)
          .textContent;
        const url = URL.createObjectURL(
          new Blob([content], { type: "text/plain;charset=utf-8" }),
        );
        const link = document.createElement("a");
        link.href = url;
        link.download = filename(selected);
        document.body.append(link);
        link.click();
        link.remove();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
      });
      toolbar.prepend(button);
    });
  }
  function start() {
    install();
    // Native reference tabs can insert examples after the initial page load.
    new MutationObserver((records) => {
      if (
        records.some((record) =>
          Array.from(record.addedNodes).some(
            (node) =>
              node.nodeType === 1 &&
              (node.matches(".av-code-wrap, pre:not([data-download-ready])") ||
                node.querySelector("pre:not([data-download-ready])")),
          ),
        )
      )
        install();
    }).observe(document.body, { childList: true, subtree: true });
  }
  if (document.readyState === "complete") start();
  else window.addEventListener("load", start, { once: true });
})();

/* Keep long source listings readable without making guide pages endlessly tall. */
(() => {
  function refresh() {
    document
      .querySelectorAll(".av-code-wrap, .wiki-download-block")
      .forEach((wrap) => {
        const pre = Array.from(wrap.querySelectorAll(":scope > pre")).find(
          (node) => !node.hidden,
        );
        if (!pre || pre.closest("[hidden]")) return;
        let toggle = wrap.querySelector(":scope > .wiki-code-expand");
        if (!toggle) {
          toggle = document.createElement("button");
          toggle.type = "button";
          toggle.className = "wiki-code-expand";
          toggle.setAttribute("aria-expanded", "false");
          toggle.textContent = "Expand full code";
          toggle.addEventListener("click", () => {
            const expanded = wrap.classList.toggle("wiki-code-expanded");
            toggle.setAttribute("aria-expanded", String(expanded));
            toggle.textContent = expanded
              ? "Collapse code"
              : "Expand full code";
            if (!expanded && wrap.getBoundingClientRect().top < 0)
              wrap.scrollIntoView({ block: "center" });
          });
          wrap.append(toggle);
        }
        const needed = pre.scrollHeight > 300;
        wrap.classList.toggle("wiki-code-collapsible", needed);
        toggle.hidden = !needed;
      });
  }
  window.addEventListener(
    "load",
    () => {
      refresh();
      setTimeout(refresh, 100);
    },
    { once: true },
  );
  document.addEventListener("change", () => requestAnimationFrame(refresh));
  document.addEventListener("wiki-examples-changed", () =>
    requestAnimationFrame(refresh),
  );
})();

/* Keep the native documentation search in the main navigation row. */
(() => {
  function integrateSearch() {
    const header = document.getElementById("navbar-top");
    const content = header?.querySelector(".nav-content");
    const search = document.querySelector(".sub-nav .nav-list-search");
    if (!content || !search) return;
    search.classList.add("av-header-search");
    content.append(search);
    const input = search.querySelector("#search-input");
    const placeholder = input.getAttribute("placeholder");
    const compact = matchMedia("(max-width: 600px)");
    const label = () => {
      input.placeholder = compact.matches ? "Search docs" : placeholder;
    };
    compact.addEventListener("change", label);
    label();
    const sub = document.querySelector(".sub-nav");
    if (sub && !sub.querySelector(".sub-nav-list li")) {
      sub.hidden = true;
      document.documentElement.style.setProperty("--sub-nav-height", "0px");
    }
  }
  if (document.readyState === "loading")
    document.addEventListener("DOMContentLoaded", integrateSearch);
  else integrateSearch();
})();
