(ns office.graph-test
  (:require [clojure.test :refer [deftest is]]
            [office.embed :as embed]
            [office.export :as export]
            [office.graph :as graph]
            [office.opc :as opc]
            [office.visual :as visual])
  (:import [java.io ByteArrayOutputStream]
           [java.util.zip ZipEntry ZipOutputStream]))

(defn zip-bytes [entries]
  (let [out (ByteArrayOutputStream.)]
    (with-open [zip (ZipOutputStream. out)]
      (doseq [[path text] entries]
        (.putNextEntry zip (ZipEntry. path))
        (.write zip (.getBytes text "UTF-8"))
        (.closeEntry zip)))
    (.toByteArray out)))

(deftest reads-pptx-parts-as-edn-graph
  (let [bytes (zip-bytes {"[Content_Types].xml" "<Types/>"
                          "ppt/slides/slide1.xml" "<p:sld><a:t>Hello</a:t><a:t>World</a:t></p:sld>"})
        pkg (opc/open-package bytes)
        g (graph/package-graph pkg)]
    (is (= :pptx (:office/kind pkg)))
    (is (= ["ppt/slides/slide1.xml"] (map :office/path (opc/office-parts pkg))))
    (is (= ["Hello" "World"]
           (->> (:office/nodes g)
                (filter #(= :text (:office/kind %)))
                (map :office/text))))))

(deftest package-graph-tolerates-missing-package-kind
  (let [g (graph/package-graph {:office/entries {"ppt/slides/slide1.xml" "<p:sld/>"}})
        root (first (:office/nodes g))]
    (is (= :unknown (:office/kind root)))
    (is (= "unknown" (:office/label root)))))

(deftest part-graph-tolerates-missing-path-and-xml
  (let [g (graph/part-graph {})
        node (first (:office/nodes g))]
    (is (= "unknown-part" (:office/id node)))
    (is (= :part (:office/kind node)))
    (is (empty? (filter #(= :text (:office/kind %)) (:office/nodes g))))))

(deftest embeds-edn-graph-non-destructively
  (let [bytes (zip-bytes {"[Content_Types].xml" "<Types/>"
                          "_rels/.rels" "<Relationships/>"
                          "ppt/slides/slide1.xml" "<p:sld><a:t>Hello</a:t></p:sld>"})
        out (embed/embed-bytes bytes)
        pkg (opc/open-package out)
        payload (embed/read-payload pkg)
        g (embed/read-graph pkg)]
    (is (= 1 (:office/version payload)))
    (is (contains? (:office/entries pkg) "ocz/causal.edn"))
    (is (= "Hello" (->> (:office/nodes g)
                        (filter #(= :text (:office/kind %)))
                        first
                        :office/text)))
    (is (re-find #"digraph office" (export/export g :dot)))
    (is (re-find #"<svg" (visual/graph-svg g)))))

(deftest embeds-payload-with-missing-package-metadata
  (let [bytes (zip-bytes {"ppt/slides/slide1.xml" "<p:sld/>"})
        pkg (opc/open-package (embed/embed-bytes bytes))]
    (is (contains? (:office/entries pkg) "[Content_Types].xml"))
    (is (contains? (:office/entries pkg) "_rels/.rels"))
    (is (contains? (:office/entries pkg) embed/payload-part))
    (is (re-find #"Extension=\"edn\"" (get-in pkg [:office/entries "[Content_Types].xml"])))
    (is (re-find #"rIdKotobaOffice" (get-in pkg [:office/entries "_rels/.rels"])))))

(deftest embedding-is-idempotent-for-package-metadata
  (let [bytes (zip-bytes {"[Content_Types].xml" "<Types/>"
                          "_rels/.rels" "<Relationships/>"
                          "ppt/slides/slide1.xml" "<p:sld/>"})
        pkg (opc/open-package (-> bytes embed/embed-bytes embed/embed-bytes))
        content-types (get-in pkg [:office/entries "[Content_Types].xml"])
        rels (get-in pkg [:office/entries "_rels/.rels"])]
    (is (= 1 (count (re-seq #"Extension=\"edn\"" content-types))))
    (is (= 1 (count (re-seq #"rIdKotobaOffice" rels))))))

(deftest embedding-detects-single-quoted-package-metadata
  (let [bytes (zip-bytes {"[Content_Types].xml" "<Types><Default Extension='edn' ContentType='application/edn'/></Types>"
                          "_rels/.rels" "<Relationships><Relationship Id='rIdKotobaOffice' Target='ocz/causal.edn'/></Relationships>"
                          "ppt/slides/slide1.xml" "<p:sld/>"})
        pkg (opc/open-package (embed/embed-bytes bytes))
        content-types (get-in pkg [:office/entries "[Content_Types].xml"])
        rels (get-in pkg [:office/entries "_rels/.rels"])]
    (is (= 1 (count (re-seq #"Extension=['\"]edn['\"]" content-types))))
    (is (= 1 (count (re-seq #"Id=['\"]rIdKotobaOffice['\"]" rels))))))

(deftest detects-package-kind-and-office-parts
  (let [xlsx (opc/open-package (zip-bytes {"xl/worksheets/sheet1.xml" "<worksheet><t>A</t></worksheet>"}))
        docx (opc/open-package (zip-bytes {"word/document.xml" "<w:document><w:t>Doc</w:t></w:document>"}))
        opc (opc/open-package (zip-bytes {"custom/item.xml" "<x/>"}))]
    (is (= :xlsx (:office/kind xlsx)))
    (is (= :docx (:office/kind docx)))
    (is (= :opc (:office/kind opc)))
    (is (= ["xl/worksheets/sheet1.xml"] (map :office/path (opc/office-parts xlsx))))
    (is (= ["word/document.xml"] (map :office/path (opc/office-parts docx))))
    (is (empty? (opc/office-parts opc)))))

(deftest office-parts-use-natural-number-order
  (let [pkg (opc/open-package
             (zip-bytes {"ppt/slides/slide10.xml" "<p:sld/>"
                         "ppt/slides/slide2.xml" "<p:sld/>"
                         "ppt/slides/slide1.xml" "<p:sld/>"}))]
    (is (= ["ppt/slides/slide1.xml"
            "ppt/slides/slide2.xml"
            "ppt/slides/slide10.xml"]
           (map :office/path (opc/office-parts pkg))))))

(deftest office-parts-natural-order-does-not-parse-huge-numbers
  (let [pptx (opc/open-package
              (zip-bytes {"ppt/slides/slide999999999999999999999999999999.xml" "<p:sld/>"
                          "ppt/slides/slide2.xml" "<p:sld/>"}))
        xlsx (opc/open-package
              (zip-bytes {"xl/worksheets/sheet999999999999999999999999999999.xml" "<worksheet/>"
                          "xl/worksheets/sheet2.xml" "<worksheet/>"}))]
    (is (= ["ppt/slides/slide2.xml"
            "ppt/slides/slide999999999999999999999999999999.xml"]
           (map :office/path (opc/office-parts pptx))))
    (is (= ["xl/worksheets/sheet2.xml"
            "xl/worksheets/sheet999999999999999999999999999999.xml"]
           (map :office/path (opc/office-parts xlsx))))))

(deftest extracts-text-from-document-and-sheet-parts
  (let [bytes (zip-bytes {"word/document.xml" "<w:document><w:t>Alpha</w:t></w:document>"
                          "xl/worksheets/sheet1.xml" "<worksheet><t>Beta</t></worksheet>"})
        g (graph/analyze-bytes bytes)]
    (is (= ["Alpha" "Beta"]
           (->> (:office/nodes g)
                (filter #(= :text (:office/kind %)))
                (map :office/text)
                sort)))))

(deftest decodes-common-xml-entities-in-text-runs
  (let [bytes (zip-bytes {"ppt/slides/slide1.xml"
                          "<p:sld><a:t>Tom &amp; Jerry</a:t><a:t>3 &lt; 4</a:t><a:t>&quot;Q&quot;</a:t></p:sld>"})
        g (graph/analyze-bytes bytes)]
    (is (= ["Tom & Jerry" "3 < 4" "\"Q\""]
           (->> (:office/nodes g)
                (filter #(= :text (:office/kind %)))
                (map :office/text))))))

(deftest decodes-numeric-xml-entities-in-text-runs
  (let [bytes (zip-bytes {"ppt/slides/slide1.xml"
                          "<p:sld><a:t>Line&#10;Break</a:t><a:t>&#x2022; bullet</a:t></p:sld>"})
        g (graph/analyze-bytes bytes)]
    (is (= ["Line\nBreak" "• bullet"]
           (->> (:office/nodes g)
                (filter #(= :text (:office/kind %)))
                (map :office/text))))))

(deftest extracts-powerpoint-text-runs-with-attributes
  (let [bytes (zip-bytes {"ppt/slides/slide1.xml"
                          "<p:sld><a:t xml:space=\"preserve\">  Kept space  </a:t></p:sld>"})
        g (graph/analyze-bytes bytes)]
    (is (= ["  Kept space  "]
           (->> (:office/nodes g)
                (filter #(= :text (:office/kind %)))
                (map :office/text))))))

(deftest package-roundtrip-preserves-binary-and-updates-text
  (let [bytes (zip-bytes {"ppt/slides/slide1.xml" "<p:sld><a:t>Old</a:t></p:sld>"
                          "ppt/media/image1.bin" "raw-binary"})
        pkg (-> (opc/open-package bytes)
                (assoc-in [:office/entries "ppt/slides/slide1.xml"] "<p:sld><a:t>New</a:t></p:sld>"))
        roundtrip (opc/open-package (opc/package-bytes pkg))]
    (is (= "<p:sld><a:t>New</a:t></p:sld>"
           (get-in roundtrip [:office/entries "ppt/slides/slide1.xml"])))
    (is (= (vec (.getBytes "raw-binary" "UTF-8"))
           (vec (get-in roundtrip [:office/raw "ppt/media/image1.bin"]))))))

(deftest package-roundtrip-preserves-empty-text-entries
  (let [bytes (zip-bytes {"ppt/slides/slide1.xml" "<p:sld/>"})
        pkg (-> (opc/open-package bytes)
                (assoc-in [:office/entries "ppt/slides/slide1.xml"] ""))
        roundtrip (opc/open-package (opc/package-bytes pkg))]
    (is (= "" (get-in roundtrip [:office/entries "ppt/slides/slide1.xml"])))))

(deftest graphml-escapes-labels-and-text
  (let [g {:office/nodes [{:office/id "n&1"
                           :office/kind :text
                           :office/label "A < B"
                           :office/text "Tom & Jerry"}]
           :office/edges [{:office/from "n&1"
                           :office/to "n<2"
                           :office/edge :contains}]}
        graphml (export/export g :graphml)]
    (is (re-find #"id=\"n&amp;1\"" graphml))
    (is (re-find #"source=\"n&amp;1\"" graphml))
    (is (re-find #"target=\"n&lt;2\"" graphml))
    (is (re-find #"A &lt; B" graphml))
    (is (re-find #"Tom &amp; Jerry" graphml))))

(deftest graphml-escapes-kind-values
  (let [g {:office/nodes [{:office/id "n1"
                           :office/kind "text<bad"
                           :office/label "Node"}]
           :office/edges [{:office/from "n1"
                           :office/to "n1"
                           :office/edge "contains&bad"}]}
        graphml (export/export g :graphml)]
    (is (re-find #"<data key=\"kind\">text&lt;bad</data>" graphml))
    (is (re-find #"<data key=\"kind\">contains&amp;bad</data>" graphml))))

(deftest export-helpers-tolerate-nil-graph
  (is (= {:office/nodes 0
          :office/edges 0
          :office/kinds {}
          :office/edge-kinds {}}
         (export/stats nil)))
  (is (re-find #"<graphml" (export/export nil :graphml)))
  (is (re-find #"digraph office" (export/export nil :dot))))

(deftest dot-escapes-quoted-strings
  (let [g {:office/nodes [{:office/id "n\"1"
                           :office/kind :text
                           :office/label "Line 1\nLine \"2\" \\ end"}]
           :office/edges [{:office/from "n\"1"
                           :office/to "n\\2"
                           :office/edge :contains}]}
        dot (export/export g :dot)]
    (is (re-find #"\"n\\\"1\"" dot))
    (is (re-find #"Line 1\\nLine \\\"2\\\" \\\\ end" dot))
    (is (re-find #"\"n\\\\2\"" dot))))

(deftest graph-exports-tolerate-missing-kind
  (let [g {:office/nodes [{:office/id "n1"
                           :office/label "No kind"}]
           :office/edges [{:office/from "n1"
                           :office/to "n1"}]}]
    (is (re-find #"unknown" (export/export g :dot)))
    (is (re-find #"<data key=\"kind\">unknown</data>" (export/export g :graphml)))))

(deftest graph-svg-escapes-text-content
  (let [g {:office/nodes [{:office/id "n1"
                           :office/kind :text
                           :office/label "A < B"
                           :office/text "Tom & Jerry"}]
           :office/edges []}
        svg (visual/graph-svg g)]
    (is (re-find #"Tom &amp; Jerry" svg))
    (is (not (re-find #"Tom & Jerry" svg)))))

(deftest graph-svg-tolerates-missing-kind
  (let [svg (visual/graph-svg {:office/nodes [{:office/id "n1"
                                               :office/label "No kind"}]
                               :office/edges []})]
    (is (re-find #"unknown" svg))
    (is (re-find #"No kind" svg))))

(deftest graph-svg-tolerates-nil-and-non-map-nodes
  (is (re-find #"<svg" (visual/graph-svg nil)))
  (let [svg (visual/graph-svg {:office/nodes ["bad <node>"]
                               :office/edges []})]
    (is (re-find #"unknown" svg))
    (is (re-find #"bad &lt;node&gt;" svg))))
