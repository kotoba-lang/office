(ns office.graph
  "OOXML package to portable EDN graph."
  (:require [clojure.string :as str]
            [office.opc :as opc]))

(defn- node [id kind label attrs]
  (merge {:office/id id
          :office/kind kind
          :office/label label}
         attrs))

(defn- edge [from to kind]
  {:office/from from
   :office/to to
   :office/edge kind})

(defn- text-runs [xml]
  (->> (re-seq #"<a:t>([^<]*)</a:t>|<w:t[^>]*>([^<]*)</w:t>|<t[^>]*>([^<]*)</t>" xml)
       (map (fn [[_ a b c]] (or a b c)))
       (remove str/blank?)
       vec))

(defn- part-kind [path]
  (cond
    (str/starts-with? path "ppt/slides/") :slide
    (str/starts-with? path "xl/worksheets/") :sheet
    (= path "word/document.xml") :document
    :else :part))

(defn part-graph [part]
  (let [path (:office/path part)
        xml (:office/xml part)
        id path
        texts (text-runs xml)
        text-nodes (map-indexed
                    (fn [idx text]
                      (node (str path "#text-" (inc idx))
                            :text
                            text
                            {:office/source path
                             :office/text text}))
                    texts)]
    {:office/nodes (into [(node id (part-kind path) path {:office/source path})]
                         text-nodes)
     :office/edges (mapv #(edge id (:office/id %) :contains) text-nodes)}))

(defn package-graph [pkg]
  (let [parts (opc/office-parts pkg)
        graphs (map part-graph parts)
        root (node "package" (:office/kind pkg) (name (:office/kind pkg)) {})]
    {:office/kind (:office/kind pkg)
     :office/nodes (vec (cons root (mapcat :office/nodes graphs)))
     :office/edges (vec (concat
                         (map #(edge "package" (:office/path %) :contains) parts)
                         (mapcat :office/edges graphs)))}))

(defn analyze-bytes [bytes]
  (-> bytes opc/open-package package-graph))
