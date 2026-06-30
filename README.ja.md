# office

[English](README.md)

Microsoft Office Open XML package のための pure CLJC / EDN runtime です。

`.pptx`, `.xlsx`, `.docx` を zip package として読み、主要 OOXML part を
portable な EDN graph に抽出します。抽出した graph は既存 Office XML を書き換えず
`ocz/causal.edn` として package に同梱できます。

## 実行環境

- 実装: Clojure / ClojureScript portable `.cljc`
- zip read/write: JVM Clojure host
- データ表現: `:office/*` の EDN map
- JavaScript / TypeScript runtime: なし

## API

```clojure
(require '[office.opc :as opc]
         '[office.graph :as graph]
         '[office.embed :as embed]
         '[office.export :as export]
         '[office.visual :as visual])

(def pkg (opc/open-package bytes))
(def g (graph/package-graph pkg))

(export/stats g)
(visual/graph-svg g)

(def embedded-bytes (embed/embed-bytes bytes))
```

## Test

```bash
clojure -X:test
```
