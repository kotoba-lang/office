(ns office.cli-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [office.cli :as cli]
            [office.embed :as embed]
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

(deftest usage-lists-commands
  (let [usage-fn (ns-resolve 'office.cli 'usage)
        usage (@usage-fn)]
    (is (re-find #"read" usage))
    (is (re-find #"graph" usage))
    (is (re-find #"embed" usage))
    (is (re-find #"svg" usage))))

(deftest package-bin-points-to-cljs-wrapper
  (let [package-json (slurp "package.json")
        bin (java.io.File. "bin/kotoba-office.cljs")]
    (is (re-find #"\"kotoba-office\"\s*:\s*\"bin/kotoba-office\.cljs\"" package-json))
    (is (not (re-find #"bin/kotoba-office\.js" package-json)))
    (is (.exists bin))
    (is (.canExecute bin))))

(deftest require-file-validates-required-path
  (let [require-file-fn (ns-resolve 'office.cli 'require-file)]
    (is (= "deck.pptx" (@require-file-fn "deck.pptx")))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"office cli"
                          (@require-file-fn nil)))))

(deftest read-command-prints-package-summary
  (let [bytes (zip-bytes {"ppt/slides/slide1.xml" "<p:sld><a:t>Hello</a:t></p:sld>"})
        printed (with-out-str
                  (with-redefs [office.cli/read-bytes (fn [path]
                                                   (is (= "deck.pptx" path))
                                                   bytes)]
                    (cli/-main "read" "deck.pptx")))
        summary (edn/read-string printed)]
    (is (= :pptx (:office/kind summary)))
    (is (= 1 (:office/entries summary)))
    (is (= ["ppt/slides/slide1.xml"] (:office/parts summary)))))

(deftest graph-command-prints-selected-format
  (let [bytes (zip-bytes {"ppt/slides/slide1.xml" "<p:sld><a:t>Hello</a:t></p:sld>"})
        printed (with-out-str
                  (with-redefs [office.cli/read-bytes (fn [_] bytes)]
                    (cli/-main "graph" "deck.pptx" "graphml")))]
    (is (re-find #"<graphml" printed))
    (is (re-find #"Hello" printed))))

(deftest embed-command-writes-package-bytes
  (let [bytes (zip-bytes {"ppt/slides/slide1.xml" "<p:sld/>"})
        written (atom nil)
        printed (with-out-str
                  (with-redefs [office.cli/read-bytes (fn [path]
                                                   (is (= "in.pptx" path))
                                                   bytes)
                                 office.cli/write-bytes! (fn [path bs]
                                                     (reset! written [path bs]))]
                    (cli/-main "embed" "in.pptx" "out.pptx")))
        summary (edn/read-string printed)]
    (is (= {:office/path "out.pptx"} summary))
    (is (= "out.pptx" (first @written)))
    (is (bytes? (second @written)))
    (is (contains? (:office/entries (opc/open-package (second @written)))
                   embed/payload-part))))

(deftest svg-command-writes-graph-svg
  (let [bytes (zip-bytes {"ppt/slides/slide1.xml" "<p:sld><a:t>Hello</a:t></p:sld>"})
        out (java.io.File/createTempFile "office-cli" ".svg")]
    (try
      (with-redefs [office.cli/read-bytes (fn [_] bytes)]
        (let [printed (with-out-str (cli/-main "svg" "deck.pptx" (.getAbsolutePath out)))
              summary (edn/read-string printed)]
          (is (= (.getAbsolutePath out) (:office/path summary)))
          (is (re-find #"<svg" (slurp out)))
          (is (re-find #"Hello" (slurp out)))))
      (finally
        (.delete out)))))
