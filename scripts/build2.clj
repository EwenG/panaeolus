(ns build2
  (:require [badigeon.bundle :as bundle]
            [badigeon.compile :as compile]
            [badigeon.clean :as clean]
            [badigeon.jar :as jar]
            [clojure.java.io :as io]
            [clojure.tools.deps.alpha.reader :as deps-reader])
  (:import [java.nio.file Paths Files]))

(def +version+ "0.4.0-SNAPSHOT")

(defn- get-os
  "Return the OS as a keyword. One of :windows :linux :mac"
  []
  (let [os (System/getProperty "os.name")]
    (cond
      (re-find #"[Ww]indows" os) :windows
      (re-find #"[Ll]inux" os)   :linux
      (re-find #"[Mm]ac" os)     :mac)))

(defn extract-jna-natives [natives-path]
  (case (get-os)
    :windows (bundle/extract-native-dependencies
              "target/classes"
              {:deps-map (deps-reader/slurp-deps "deps.edn")
               :native-prefixes {'net.java.dev.jna/jna "com/sun/jna/win32-x86-64"}
               :native-path "com/sun/jna/win32-x86-64"})
    :linux (bundle/extract-native-dependencies
            "target/classes"
            {:deps-map (deps-reader/slurp-deps "deps.edn")
             :native-prefixes {'net.java.dev.jna/jna "com/sun/jna/linux-x86-64"}
             :native-path "com/sun/jna/linux-x86-64"})
    :mac (bundle/extract-native-dependencies
          "target/classes"
          {:deps-map (deps-reader/slurp-deps "deps.edn")
           :native-prefixes {'net.java.dev.jna/jna "com/sun/jna/darwin"}
           :native-path "com/sun/jna/darwin"})))

(defn -main []
  (clean/clean "target")
  (compile/compile '[panaeolus.all clojure.core.specs.alpha]
                   {:compile-path "target/classes"
                    :compiler-options {:disable-locals-clearing false
                                       :direct-linking true}})
  (let [jar-path (jar/jar 'panaeolus
                          {:mvn/version +version+}
                          {:paths ["resources" "target/classes"]
                           :allow-all-dependencies? true})
        jar-file-name (.getFileName (Paths/get jar-path (make-array String 0)))
        deps-map (-> (deps-reader/slurp-deps "deps.edn")
                     (assoc :paths [])
                     (update :deps dissoc 'badigeon/badigeon))
        out-path (bundle/make-out-path 'panaeolus +version+)]
    (bundle/bundle out-path {:deps-map deps-map})
    (io/copy (io/file jar-path) (io/file (-> out-path
                                             (.resolve "lib")
                                             (.resolve jar-file-name)
                                             str)))
    (extract-jna-natives out-path)
    (bundle/bin-script out-path 'panaeolus.all))
  
  (System/exit 0))
