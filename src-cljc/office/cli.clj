(ns office.cli
  (:require [office.embed :as embed]
            [office.export :as export]
            [office.graph :as graph]
            [office.opc :as opc]
            [office.visual :as visual])
  (:gen-class))

(defn- usage []
  (str "office cli\n\n"
       "Commands:\n"
       "  read <file>                         print package summary\n"
       "  graph <file> [edn|dot|graphml]       print extracted graph\n"
       "  embed <file> <out-file>              embed ocz/causal.edn\n"
       "  svg <file> <out.svg>                 write graph inspection SVG\n"))

(defn- read-bytes [path]
  (java.nio.file.Files/readAllBytes
   (java.nio.file.Path/of (str path) (into-array String []))))

(defn- write-bytes! [path bs]
  (java.nio.file.Files/write
   (java.nio.file.Path/of (str path) (into-array String []))
   bs
   (into-array java.nio.file.OpenOption [])))

(defn- require-file [file]
  (when-not file
    (throw (ex-info (usage) {})))
  file)

(defn -main [& args]
  (try
    (case (first args)
      "read" (let [[_ file] args
                   file (require-file file)
                   pkg (opc/open-package (read-bytes file))]
               (prn {:office/kind (:office/kind pkg)
                     :office/entries (count (:office/entries pkg))
                     :office/parts (mapv :office/path (opc/office-parts pkg))}))
      "graph" (let [[_ file fmt] args
                    file (require-file file)
                    g (graph/analyze-bytes (read-bytes file))]
                (println (export/export g (keyword (or fmt "edn")))))
      "embed" (let [[_ file out] args]
                (when-not (and file out) (throw (ex-info (usage) {})))
                (write-bytes! out (embed/embed-bytes (read-bytes file)))
                (prn {:office/path out}))
      "svg" (let [[_ file out] args]
              (when-not (and file out) (throw (ex-info (usage) {})))
              (spit out (visual/graph-svg (graph/analyze-bytes (read-bytes file))))
              (prn {:office/path out}))
      (println (usage)))
    (catch Exception e
      (binding [*out* *err*]
        (println (.getMessage e)))
      (System/exit 1))))
