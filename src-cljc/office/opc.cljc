(ns office.opc
  "Small JVM-backed OPC reader for OOXML packages."
  (:require [clojure.string :as str])
  #?(:clj (:import [java.io ByteArrayInputStream]
                   [java.util.zip ZipInputStream])))

(def office-part-pattern
  #"^(ppt/slides/slide\d+|xl/worksheets/sheet\d+|word/document)\.xml$")

(defn package-kind [entries]
  (cond
    (some #(str/starts-with? % "ppt/") (keys entries)) :pptx
    (some #(str/starts-with? % "xl/") (keys entries)) :xlsx
    (some #(str/starts-with? % "word/") (keys entries)) :docx
    :else :opc))

#?(:clj
   (defn- read-entry [^ZipInputStream zip]
     (let [buf (byte-array 8192)
           out (java.io.ByteArrayOutputStream.)]
       (loop []
         (let [n (.read zip buf)]
           (when (pos? n)
             (.write out buf 0 n)
             (recur))))
       (.toString out "UTF-8"))))

(defn open-package
  "Reads OOXML bytes into an EDN package map with UTF-8 XML entries."
  [bytes]
  #?(:clj
     (with-open [zip (ZipInputStream. (ByteArrayInputStream. bytes))]
       (loop [entries {}]
         (if-let [entry (.getNextEntry zip)]
           (let [name (.getName entry)
                 text? (or (str/ends-with? name ".xml")
                           (str/ends-with? name ".rels"))]
             (recur (if text?
                      (assoc entries name (read-entry zip))
                      entries)))
           {:office/kind (package-kind entries)
            :office/entries entries})))
     :cljs
     (throw (ex-info "open-package requires a host zip implementation" {:feature :office/opc}))))

(defn office-parts [pkg]
  (->> (:office/entries pkg)
       (filter (fn [[path _]] (re-matches office-part-pattern path)))
       (map (fn [[path xml]]
              {:office/path path
               :office/xml xml}))
       (sort-by :office/path)
       vec))
