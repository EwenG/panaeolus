(ns build
  (:require [badigeon.bundle :as bundle]
            [badigeon.classpath :as classpath]
            [badigeon.clean :as clean]
            [badigeon.compile :as compile]
            [badigeon.clean :as clean]
            [clojure.java.io :as io]
            [clojure.tools.deps.alpha.reader :as deps-reader]))

(defn -main []
  (clean/clean "target")
  (compile/compile '[panaeolus.all clojure.core.specs.alpha]
                   {:compile-path "target/classes"
                    :compiler-options {:disable-locals-clearing false
                                       :direct-linking true}
                    :classpath (classpath/make-classpath {:aliases []})})
  (compile/extract-classes-from-dependencies
   {:deps-map (deps-reader/slurp-deps "deps.edn")})
  (spit "target/classes/clojure/version.properties"
        (slurp (io/resource "clojure/version.properties"))))
