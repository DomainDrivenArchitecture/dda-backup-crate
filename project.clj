(defproject org.domaindrivenarchitecture/dda-backup-crate "0.2.2-SNAPSHOT"
  :description "A crate to handle configuration, their dependencies, documentation and validation"
  :url "https://www.domaindrivenarchitecture.org"
  :license {:name "Apache License, Version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0.html"}
 :dependencies [[org.clojure/clojure "1.6.0"]
                [com.palletops/pallet "0.8.10"]
                [com.palletops/pallet "0.8.10" :classifier "tests"]
                [com.palletops/stevedore "0.8.0-beta.7"]
                [org.domaindrivenarchitecture.org/dda-config-crate "0.3.0-SNAPSHOT"]]
 :repositories [["snapshots" :clojars]
                ["releases" :clojars]]
 :deploy-repositories [["snapshots" :clojars]
                       ["releases" :clojars]]
 :profiles {:dev
            {:dependencies
             [[com.palletops/pallet "0.8.10" :classifier "tests"]
              ]
             :plugins
             [[com.palletops/pallet-lein "0.8.0-alpha.1"]]}
             :leiningen/reply
              {:dependencies [[org.slf4j/jcl-over-slf4j "1.7.2"]]
               :exclusions [commons-logging]}}
 :local-repo-classpath true
 :classifiers {:tests {:source-paths ^:replace ["test"]
                       :resource-paths ^:replace []}})
 
  
