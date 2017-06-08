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
   [clojure.string :as str]
   [schema.core :as s]
   [schema-tools.core :as st]
   [pallet.actions :as actions]
   [pallet.stevedore :as stevedore]
   [org.domaindrivenarchitecture.pallet.crate.backup.backup-element :as element]))

(defn install []
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

;TODO: make the paths-gets able to handle more than one element
(defn configure [config]
  (let [trust-script-path (get ((get config :elements) 0) :trust-script-path)
        priv-key-path (get ((get config :elements) 0) :priv-key-path)
        pub-key-path (get ((get config :elements) 0) :pub-key-path)
        passphrase (get((get config :elements) 0) :passphrase)]
    (actions/remote-file "/var/opt/backup/dup_pub.key" :local-file pub-key-path :owner "root", :group "users" :mode "700"
                         :action :create :force true)
    (actions/remote-file "/var/opt/backup/dup_priv.key" :local-file priv-key-path :owner "root", :group "users" :mode "700"
                         :action :create :force true)
    (actions/directory "~/.gnupg" :action :create :owner "root", :group "users" :mode "700")
    (actions/exec-script* "cd ~/.gnupg && echo \"allow-loopback-pinentry\"  >> gpg-agent.conf")
    (actions/exec-script* "gpgconf --kill gpg-agent")
    (actions/exec-script* (str "gpg2 --import /var/opt/backup/dup_pub.key && (echo " passphrase " | sudo -S gpg2 --pinentry-mode loopback --batch --passphrase-fd 0 --import /var/opt/backup/dup_priv.key)"))
    (actions/remote-file "/var/opt/backup/trust.sh" :local-file trust-script-path :owner "root", :group "users" :mode "700"
                         :action :create :force true)
    (actions/exec-script* "/bin/bash /var/opt/backup/trust.sh")))

;TODO: catch options whose delimiter is not empty-space but =
(s/defn ^:always-validate option-parser [options :- element/DuplicityOptions]
  (apply str (keep-indexed (fn [index item]
                             (if (even? index)
                               (str " --" (name item))
                               (if (= item true)
                                 ""
                                 (str " " (name item)))))
                           options)))

(def DuplicityModus (s/enum :backup :restore))

(defn prep-dup-script [element backup]
  (cond
    (and (contains? (get element :prep-scripts) :prep-backup-script) backup)
    (get-in element [:prep-scripts :prep-backup-script])
    (and (contains? (get element :prep-scripts) :prep-restore-script) (not backup))
    (get-in element [:prep-scripts :prep-restore-script])))

(defn main-dup-script [element backup]
  (str "/usr/bin/duplicity "
       (if backup (name (get element :action))
           "restore")
       (str " --gpg-binary gpg2")
       (when (contains? element :options)
         (cond
           (and (contains? (get element :options) :backup-options) backup)
           (str (option-parser (get-in element [:options :backup-options])) " " (get element :directory) " " (get element :url))
           (and (contains? (get element :options) :restore-options) (not backup))
           (str (option-parser (get-in element [:options :restore-options])) " " (get element :url) " " (get element :directory))))))

(defn post-dup-script [element backup]
  (cond
    (and (contains? (get element :post-ops) :remove-remote-backup) backup)
    (let [remove-options (get-in element [:post-ops :remove-remote-backup])]
      (str "/usr/bin/duplicity remove-older-than "
           (get remove-options :days) "D"
           (str " --gpg-binary gpg2")
           (option-parser (get remove-options :options))
           " " (get element :url)))
    (and (contains? (get element :post-ops) :post-transport-script) (not backup))
    (get-in element [:post-ops :post-transport-script])))

;TODO: implement support for other cloud than aws
(s/defn ^:always-validate duplicity-parser
  [element :- element/BackupElement
   modus :- DuplicityModus]
  (let [aws (contains? element :aws-access-key-id)
        backup (= modus :backup)]
    [(if backup
       "#backup the files"
       "# Transport Backup")
     (str "export PASSPHRASE=" (get element :passphrase))
     (str "export TMPDIR=" (get element :tmp-dir))
     (when aws (str "export AWS_ACCESS_KEY_ID=" (get element :aws-access-key-id)))
     (when aws (str "export AWS_SECRET_ACCESS_KEY=" (get element :aws-secret-access-key)))
     (when aws (str "export S3_USE_SIGV4=" (get element :s3-use-sigv4)))
     (when (contains? element :prep-scripts)
       (prep-dup-script element backup))
     (main-dup-script element backup)
     (when (contains? element :post-ops)
       (post-dup-script element backup))
     (when aws (str "unset AWS_ACCESS_KEY_ID"))
     (when aws (str "unset AWS_SECRET_ACCESS_KEY"))
     (when aws (str "unset S3_USE_SIGV4"))
     (str "unset PASSPHRASE")
     (str "unset TMPDIR")
     ""]))

;TODO: make crate able to handle other parallel backup-elements with dup
(defn check-for-dup [partial-config]
  (and
   (contains? partial-config :elements)
   (not (empty? (get partial-config :elements)))
   (= (((get partial-config :elements) 0) :type) :duplicity)))
