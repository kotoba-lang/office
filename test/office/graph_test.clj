(ns office.graph-test
  (:require [clojure.test :refer [deftest is]]
            [office.graph :as graph]
            [office.opc :as opc])
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
