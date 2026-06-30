# office

[![CI](https://github.com/kotoba-lang/office/actions/workflows/ci.yml/badge.svg)](https://github.com/kotoba-lang/office/actions/workflows/ci.yml)

Pure CLJC / EDN runtime for Microsoft Office Open XML packages.

`office` reads `.pptx`, `.xlsx`, and `.docx` zip packages, extracts the core
OOXML parts into a portable EDN graph, and can embed that graph back into the
package as `ocz/causal.edn` without rewriting the existing Office XML parts.

## Runtime

- Implementation: Clojure / ClojureScript portable `.cljc`
- Host zip support: JVM Clojure for package read/write
- Data surface: EDN maps under `:office/*`
- JavaScript / TypeScript runtime: none

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

## Test

```bash
clojure -X:test
```
