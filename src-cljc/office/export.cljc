(ns office.export
  "Graph export helpers."
  (:require [kotoba.lang.text :as str]))

(defn- xml-esc [x]
  (-> (str (or x ""))
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")
      (str/replace "\"" "&quot;")))

(defn- dot-esc [x]
  (-> (str (or x ""))
      (str/replace "\\" "\\\\")
      (str/replace "\"" "\\\"")
      (str/replace "\r" "\\r")
      (str/replace "\n" "\\n")))

(defn- safe-name [x]
  (if x (name x) "unknown"))

(defn stats [g]
  (let [nodes (or (:office/nodes g) [])
        edges (or (:office/edges g) [])]
    {:office/nodes (count nodes)
     :office/edges (count edges)
     :office/kinds (frequencies (map :office/kind nodes))
     :office/edge-kinds (frequencies (map :office/edge edges))}))

(defn dot [g]
  (let [q #(str "\"" (dot-esc %) "\"")]
    (str "digraph office {\n"
         (apply str
                (for [{:office/keys [id kind label]} (:office/nodes g)]
                  (str "  " (q id) " [label=" (q (str (safe-name kind) "\n" label)) "];\n")))
         (apply str
                (for [{:office/keys [from to edge]} (:office/edges g)]
                  (str "  " (q from) " -> " (q to) " [label=" (q (safe-name edge)) "];\n")))
         "}\n")))

(defn graphml [g]
  (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
       "<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\"><graph edgedefault=\"directed\">"
       (apply str
              (for [{:office/keys [id kind label text]} (:office/nodes g)]
                (str "<node id=\"" (xml-esc id) "\"><data key=\"kind\">" (xml-esc (safe-name kind)) "</data>"
                     "<data key=\"label\">" (xml-esc label) "</data>"
                     (when text (str "<data key=\"text\">" (xml-esc text) "</data>"))
                     "</node>")))
       (apply str
              (map-indexed
               (fn [idx {:office/keys [from to edge]}]
                 (str "<edge id=\"e" idx "\" source=\"" (xml-esc from) "\" target=\"" (xml-esc to) "\">"
                      "<data key=\"kind\">" (xml-esc (safe-name edge)) "</data></edge>"))
               (:office/edges g)))
       "</graph></graphml>"))

(defn export [g format]
  (case format
    :edn (pr-str g)
    :dot (dot g)
    :graphml (graphml g)
    (pr-str g)))
