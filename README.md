# office

[![CI](https://github.com/kotoba-lang/office/actions/workflows/ci.yml/badge.svg)](https://github.com/kotoba-lang/office/actions/workflows/ci.yml)

Pure CLJC / EDN runtime for Microsoft Office Open XML packages.

`office` reads `.pptx`, `.xlsx`, and `.docx` zip packages, extracts the core
OOXML parts into a portable EDN graph, and can embed that graph back into the
package as `ocz/causal.edn` without rewriting the existing Office XML parts.

The reader keeps text entries as EDN-friendly strings, decodes common named and
numeric XML text entities, extracts attribute-bearing PowerPoint text runs,
orders numbered Office parts naturally, preserves non-text zip entries for
round-trip writes, tolerates malformed direct part graph inputs, escapes
DOT/GraphML/SVG inspection output, and embeds payload metadata idempotently even
when package metadata starts as self-closing XML or uses single-quoted
attributes.

## Runtime

- Implementation: Clojure / ClojureScript portable `.cljc`
- Host zip support: JVM Clojure for package read/write
- Data surface: EDN maps under `:office/*`
- JavaScript / TypeScript core runtime: none. The npm package only provides a
  thin `node` bin wrapper that invokes the Clojure CLI.

## API

```clojure
(require '[office.opc :as opc]
         '[office.graph :as graph]
         '[office.embed :as embed]
         '[office.export :as export]
         '[office.visual :as visual])

(def bytes (java.nio.file.Files/readAllBytes
            (java.nio.file.Path/of "deck.pptx" (into-array String []))))

(def pkg (opc/open-package bytes))
(def g (graph/package-graph pkg))

(export/stats g)
(export/export g :dot)
(visual/graph-svg g)

(def embedded (embed/embed-bytes bytes))
```

## Namespaces

- `office.opc`: OOXML zip reader/writer for text parts and raw round-trip
- `office.graph`: deterministic EDN graph extraction
- `office.embed`: non-destructive `ocz/causal.edn` payload embedding
- `office.export`: EDN, DOT, and GraphML export
- `office.visual`: compact SVG graph inspection

## CLI / npm

The CLI works directly with Clojure or through the npm wrapper. In both cases
`clojure` must be installed on the host.

```bash
clojure -M:cli read deck.pptx
clojure -M:cli graph deck.pptx dot
clojure -M:cli embed deck.pptx deck.ocz.pptx
clojure -M:cli svg deck.pptx graph.svg

npx @kotoba-lang/office read deck.pptx
```

## Test

```bash
clojure -X:test
```

The test suite covers package kind detection, pptx/xlsx/docx text extraction,
natural Office part ordering, common XML entity decoding, attribute-bearing
PowerPoint text runs, malformed direct part graph fallbacks, non-destructive
package round-trip behavior, CLI commands, payload embedding, DOT/GraphML/SVG escaping, missing package metadata
fallbacks, and idempotent content type / relationship updates across single- or
double-quoted attributes.
