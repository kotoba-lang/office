(ns office.export
  "Graph export helpers."
  (:require [clojure.string :as str]))

(defn stats [g]
  {:office/nodes (count (:office/nodes g))
   :office/edges (count (:office/edges g))
   :office/kinds (frequencies (map :office/kind (:office/nodes g)))
   :office/edge-kinds (frequencies (map :office/edge (:office/edges g)))})

(defn dot [g]
  (let [q #(str "\"" (str/replace (str %) "\"" "\\\\\"") "\"")]
    (str "digraph office {\n"
         (apply str
                (for [{:office/keys [id kind label]} (:office/nodes g)]
                  (str "  " (q id) " [label=" (q (str (name kind) "\\n" label)) "];\n")))
         (apply str
                (for [{:office/keys [from to edge]} (:office/edges g)]
                  (str "  " (q from) " -> " (q to) " [label=" (q (name edge)) "];\n")))
         "}\n")))

(defn graphml [g]
  (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
       "<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\"><graph edgedefault=\"directed\">"
       (apply str
              (for [{:office/keys [id kind label text]} (:office/nodes g)]
                (str "<node id=\"" id "\"><data key=\"kind\">" (name kind) "</data>"
                     "<data key=\"label\">" label "</data>"
                     (when text (str "<data key=\"text\">" text "</data>"))
                     "</node>")))
       (apply str
              (map-indexed
               (fn [idx {:office/keys [from to edge]}]
                 (str "<edge id=\"e" idx "\" source=\"" from "\" target=\"" to "\">"
                      "<data key=\"kind\">" (name edge) "</data></edge>"))
               (:office/edges g)))
       "</graph></graphml>"))

(defn export [g format]
  (case format
    :edn (pr-str g)
    :dot (dot g)
    :graphml (graphml g)
    (pr-str g)))
