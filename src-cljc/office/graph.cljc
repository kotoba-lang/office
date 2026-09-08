(ns office.graph
  "OOXML package to portable EDN graph."
  (:require [kotoba.lang.text :as str]
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

(defn- safe-name [x]
  (if x (name x) "unknown"))

(defn- parse-int-radix [s radix]
  #?(:clj (Integer/parseInt s radix)
     :cljs (js/parseInt s radix)))

(defn- codepoint-string [n]
  #?(:clj (String. (Character/toChars n))
     :cljs (.fromCodePoint js/String n)))

(defn- decode-numeric-entity [[raw hex dec]]
  (try
    (let [n (if hex
              (parse-int-radix hex 16)
              (parse-int-radix dec 10))]
      (codepoint-string n))
    (catch #?(:clj Exception :cljs :default) _
      raw)))

(defn- xml-text [x]
  (-> (str (or x ""))
      (str/replace #"&#x([0-9A-Fa-f]+);|&#([0-9]+);" decode-numeric-entity)
      (str/replace "&lt;" "<")
      (str/replace "&gt;" ">")
      (str/replace "&quot;" "\"")
      (str/replace "&apos;" "'")
      (str/replace "&amp;" "&")))

(defn- text-runs [xml]
  (->> (re-seq #"<a:t[^>]*>([^<]*)</a:t>|<w:t[^>]*>([^<]*)</w:t>|<t[^>]*>([^<]*)</t>" (or xml ""))
       (map (fn [[_ a b c]] (or a b c)))
       (map xml-text)
       (remove str/blank?)
       vec))

(defn- part-kind [path]
  (let [path (str (or path ""))]
    (cond
    (str/starts-with? path "ppt/slides/") :slide
    (str/starts-with? path "xl/worksheets/") :sheet
    (= path "word/document.xml") :document
      :else :part)))

(defn part-graph [part]
  (let [path (or (:office/path part) "unknown-part")
        xml (or (:office/xml part) "")
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
        root (node "package" (or (:office/kind pkg) :unknown) (safe-name (:office/kind pkg)) {})]
    {:office/kind (:office/kind pkg)
     :office/nodes (vec (cons root (mapcat :office/nodes graphs)))
     :office/edges (vec (concat
                         (map #(edge "package" (:office/path %) :contains) parts)
                         (mapcat :office/edges graphs)))}))

(defn analyze-bytes [bytes]
  (-> bytes opc/open-package package-graph))
