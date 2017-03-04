(defproject org.domaindrivenarchitecture/dda-backup-crate "0.4.1"
  :description "A crate to handle configuration, their dependencies, documentation and validation"
  :url "https://www.domaindrivenarchitecture.org"
  :license {:name "Apache License, Version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [prismatic/schema "1.1.3"]
                 [com.palletops/pallet "0.8.12"]
                 [com.palletops/stevedore "0.8.0-beta.7"]
                 [org.domaindrivenarchitecture/dda-config-commons "0.1.7"]
                 [org.domaindrivenarchitecture/dda-pallet-commons "0.3.0"]
                 [org.domaindrivenarchitecture/dda-pallet "0.3.1"]
                 [org.domaindrivenarchitecture/dda-config-crate "0.3.4"]]
  :repositories [["snapshots" :clojars]
                 ["releases" :clojars]]
  :deploy-repositories [["snapshots" :clojars]
                        ["releases" :clojars]]
  :profiles {:dev
             {:dependencies
              [[com.palletops/pallet "0.8.12" :classifier "tests"]
               [org.domaindrivenarchitecture/dda-pallet-commons "0.3.0" :classifier "tests"]]
              :plugins
              [[lein-sub "0.3.0"]]}
              :leiningen/reply
               {:dependencies [[org.slf4j/jcl-over-slf4j "1.7.24"]]
                :exclusions [commons-logging]}}
  :local-repo-classpath true
  :classifiers {:tests {:source-paths ^:replace ["test"]
                        :resource-paths ^:replace []}})
 
  
