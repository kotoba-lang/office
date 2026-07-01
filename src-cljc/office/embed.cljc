(ns office.embed
  "Non-destructive EDN graph embedding for OOXML packages."
  (:require [clojure.edn :as edn]
            [office.graph :as graph]
            [office.opc :as opc]
            [ooxml.core :as ooxml]))

(def payload-part "ocz/causal.edn")
(def payload-version 1)
(def payload-relationship
  (ooxml/relationship {:id "rIdKotobaOffice"
                       :type "https://kotoba-lang.org/office/relationship/causal-edn"
                       :target payload-part}))

(defn payload [g]
  {:office/version payload-version
   :office/generator "kotoba-lang/office"
   :office/graph g})

(defn- ensure-content-type [xml]
  (ooxml/ensure-content-type-extension xml "edn" "application/edn"))

(defn- ensure-root-rels [xml]
  (ooxml/ensure-root-relationship xml payload-relationship))

(defn embed-graph
  "Returns a package with graph payload embedded as ocz/causal.edn."
  ([pkg] (embed-graph pkg (graph/package-graph pkg)))
  ([pkg g]
   (-> pkg
       (update :office/entries assoc payload-part (pr-str (payload g)))
       (update-in [:office/entries "[Content_Types].xml"] ensure-content-type)
       (update-in [:office/entries "_rels/.rels"] ensure-root-rels))))

(defn read-payload [pkg]
  (some-> (get-in pkg [:office/entries payload-part])
          edn/read-string))

(defn read-graph [pkg]
  (or (:office/graph (read-payload pkg))
      (graph/package-graph pkg)))

(defn embed-bytes [bytes]
  #?(:clj
     (-> bytes opc/open-package embed-graph opc/package-bytes)
     :cljs
     (throw (ex-info "embed-bytes requires host zip support" {:feature :office/embed}))))
