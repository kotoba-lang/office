(ns office.visual
  "Small SVG renderers for CLJC graph inspection."
  (:require [clojure.string :as str]))

(defn- esc [x]
  (-> (str (or x ""))
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")
      (str/replace "\"" "&quot;")))

(defn graph-svg [g]
  (let [nodes (vec (:office/nodes g))
        h (max 160 (+ 60 (* 42 (count nodes))))]
    (str "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"960\" height=\"" h "\" viewBox=\"0 0 960 " h "\">"
         "<rect width=\"960\" height=\"" h "\" fill=\"#f7f8fb\"/>"
         "<text x=\"28\" y=\"34\" font-family=\"Inter,Arial\" font-size=\"22\" font-weight=\"700\" fill=\"#17202a\">office graph</text>"
         (apply str
                (map-indexed
                 (fn [idx {:office/keys [id kind label text]}]
                   (let [y (+ 66 (* idx 42))]
                     (str "<rect x=\"28\" y=\"" (- y 24) "\" width=\"904\" height=\"32\" rx=\"6\" fill=\"#fff\" stroke=\"#d8dee8\"/>"
                          "<text x=\"42\" y=\"" y "\" font-family=\"ui-monospace,Menlo,monospace\" font-size=\"13\" fill=\"#496b9a\">"
                          (esc (name kind)) "</text>"
                          "<text x=\"150\" y=\"" y "\" font-family=\"Inter,Arial\" font-size=\"13\" fill=\"#17202a\">"
                          (esc (or text label id)) "</text>")))
                 nodes))
         "</svg>")))
