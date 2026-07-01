(ns office.opc
  "Small JVM-backed OPC reader for OOXML packages."
  (:require [clojure.string :as str]
            [ooxml.core :as ooxml])
  #?(:clj (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
                   [java.util.zip ZipEntry ZipInputStream ZipOutputStream])))

(def office-part-pattern ooxml/office-part-pattern)

(defn package-kind [entries]
  (ooxml/package-kind entries))

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
  "Reads OOXML bytes into an EDN package map.

  Text entries are decoded under :office/entries. Every zip entry is preserved
  under :office/raw so writer operations can be non-destructive."
  [bytes]
  #?(:clj
     (with-open [zip (ZipInputStream. (ByteArrayInputStream. bytes))]
       (loop [entries {}
              raw {}]
         (if-let [entry (.getNextEntry zip)]
           (let [name (.getName entry)
                 buf (byte-array 8192)
                 out (ByteArrayOutputStream.)
                 _ (loop []
                     (let [n (.read zip buf)]
                       (when (pos? n)
                         (.write out buf 0 n)
                         (recur))))
                 entry-bytes (.toByteArray out)
                 text? (or (str/ends-with? name ".xml")
                           (str/ends-with? name ".rels")
                           (str/ends-with? name ".edn")
                           (str/ends-with? name ".json")
                           (str/ends-with? name ".jsonl"))]
             (recur (if text?
                      (assoc entries name (String. entry-bytes "UTF-8"))
                      entries)
                    (assoc raw name entry-bytes)))
           {:office/kind (package-kind entries)
            :office/entries entries
            :office/raw raw})))
     :cljs
     (throw (ex-info "open-package requires a host zip implementation" {:feature :office/opc}))))

#?(:clj
   (defn package-bytes
     "Writes a package map back to zip bytes. Entries in :office/entries replace
     matching raw entries; new text entries are added."
     [pkg]
     (let [out (ByteArrayOutputStream.)
           raw (:office/raw pkg)
           entries (:office/entries pkg)
           paths (sort (set (concat (keys raw) (keys entries))))]
       (with-open [zip (ZipOutputStream. out)]
         (doseq [path paths]
           (.putNextEntry zip (ZipEntry. path))
           (if (contains? entries path)
             (let [text (get entries path)]
               (.write zip (.getBytes (str text) "UTF-8")))
             (.write zip ^bytes (get raw path)))
           (.closeEntry zip)))
       (.toByteArray out))))

(defn office-parts [pkg]
  (mapv (fn [[path xml]]
          {:office/path path
           :office/xml xml})
        (ooxml/office-parts (:office/entries pkg))))
