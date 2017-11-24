(defproject dda/dda-backup-crate "0.7.0-SNAPSHOT"
  :description "A crate to handle backups"
  :url "https://www.domaindrivenarchitecture.org"
  :license {:name "Apache License, Version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [dda/dda-pallet "0.6.2"]
                 [dda/dda-user-crate "0.7.0"]
                 [selmer "1.11.3"]]
  :repositories [["snapshots" :clojars]
                 ["releases" :clojars]]
  :deploy-repositories [["snapshots" :clojars]
                        ["releases" :clojars]]
  :profiles {:dev
             {:source-paths ["integration"]
              :dependencies
              [[org.domaindrivenarchitecture/pallet-aws "0.2.8.2"]
               [com.palletops/pallet "0.8.12" :classifier "tests"]
               [dda/dda-pallet-commons "0.6.0-SNAPSHOT" :classifier "tests"]
               [ch.qos.logback/logback-classic "1.2.3"]
               [org.slf4j/jcl-over-slf4j "1.8.0-beta0"]]
              :plugins
              [[lein-sub "0.3.0"]]}
             :leiningen/reply
             {:dependencies [[org.slf4j/jcl-over-slf4j "1.8.0-beta0"]]
              :exclusions [commons-logging]}}
  :local-repo-classpath true
  :classifiers {:tests {:source-paths ^:replace ["test" "integration"]
                        :resource-paths ^:replace ["dev-resources"]}})
