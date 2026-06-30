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
