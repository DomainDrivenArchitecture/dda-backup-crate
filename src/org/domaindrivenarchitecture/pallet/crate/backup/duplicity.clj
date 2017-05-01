; Licensed to the Apache Software Foundation (ASF) under one
; or more contributor license agreements. See the NOTICE file
; distributed with this work for additional information
; regarding copyright ownership. The ASF licenses this file
; to you under the Apache License, Version 2.0 (the
; "License"); you may not use this file except in compliance
; with the License. You may obtain a copy of the License at
;
; http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.

(ns org.domaindrivenarchitecture.pallet.crate.backup.duplicity
  (:require
   [clojure.java.io :as io]
   [schema.core :as s]
   [schema-tools.core :as st]
   [pallet.actions :as actions]
   [pallet.stevedore :as stevedore]))

(defn install []
  ;TODO: unzip gets installed by tomcat-crate, check for existing installation
  (actions/package "unzip")
  (actions/package "rng-tools")
  (actions/package-source "duplicity"
                          :aptitude
                          {:url "ppa:duplicity-team/ppa"})
  (actions/package "duplicity")
  (actions/package "gnupg2")
  (actions/package "python3")
  (actions/remote-directory
   "/var/opt/backup/"
   :action :create
   :url "https://github.com/boto/boto/archive/2.43.0.zip"
   :unpack :unzip
   :owner "root"
   :group "users"
   :mode "755")
  (actions/exec-script* "cd /var/opt/backup/boto-2.43.0/ && /usr/bin/python setup.py install"))


(defn configure []
  ;TODO: change to get keys and trust from config
  (actions/remote-file "/var/opt/backup/9C26059F_pub.key" :local-file "/home/hel/.pallet/9C26059F_pub.key" :owner "root", :group "users" :mode "700"
  :action :create :force true)
  (actions/remote-file "/var/opt/backup/9C26059F_priv.key" :local-file "/home/hel/.pallet/9C26059F_priv.key" :owner "root", :group "users" :mode "700"
  :action :create :force true)
  (actions/exec-script* "gpg --import /var/opt/backup/9C26059F_pub.key && gpg --import /var/opt/backup/9C26059F_priv.key" )
  (actions/remote-file "/var/opt/backup/trust.sh" :local-file "/home/hel/.pallet/trust.sh" :owner "root", :group "users" :mode "700"
  :action :create :force true)
  (actions/exec-script* "/bin/bash /var/opt/backup/trust.sh")
  )
