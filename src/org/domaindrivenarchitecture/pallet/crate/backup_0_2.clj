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

(ns org.domaindrivenarchitecture.pallet.crate.backup-0-2
   (:require
    [pallet.actions :as actions]
    [pallet.stevedore :as stevedore]
    [org.domaindrivenarchitecture.pallet.crate.util :as util]
    ))

(def facility
  :dda-backup)

(def backup-user-name 
  "dataBackupSource")

(defn- script-path
  [app-name]
  (str "/usr/lib/" app-name)
  )

(defn- cron-name
  [instance-name script-type]
  (case script-type
      :backup 
      (str instance-name "_backup")
      :restore 
      (str instance-name "_restore")
      :source-transport
      (str instance-name "_source_transport")
      )
  )

(defn- script-name
  [instance-name script-type]
  (str (cron-name instance-name script-type) ".sh")
  )


(defn- script-path-with-name
  [app-name instance-name script-type]
  (str (script-path app-name) "/" (script-name instance-name script-type))
  )

(defn create-backup-source-user
  []
  (actions/user backup-user-name 
                :action :create 
                :create-home true 
                :shell :bash
                :password "WIwn6jIUt2Rbc")
  (actions/directory (str "/home/" backup-user-name "/transport-outgoing")
                     :action :create
                     :owner backup-user-name
                     :group backup-user-name)
  (actions/directory (str "/home/" backup-user-name "/store")
                     :action :create
                     :owner backup-user-name
                     :group backup-user-name)
  (actions/directory (str "/home/" backup-user-name "/restore")
                     :action :create
                     :owner backup-user-name
                     :group backup-user-name)
  )

(defn create-source-environment
  [app-name]
  (actions/directory 
    (script-path app-name)
    :action :create
    :owner "root"
    :group "root")
  )

(defn create-source-backup
  [app-name instance-name backup-lines]
  (actions/remote-file
    (script-path-with-name app-name instance-name :backup)
    :mode "700"
    :overwrite-changes true
    :literal true
    :content (util/create-file-content 
               backup-lines))
  (actions/symbolic-link 
    (script-path-with-name app-name instance-name :backup)
    (str "/etc/cron.daily/10_" (cron-name instance-name :backup))
    :action :create)
  )

(defn create-source-restore
  [app-name instance-name restore-lines]
  (actions/remote-file
    (script-path-with-name app-name instance-name :restore)  
    :mode "700"
    :overwrite-changes true
    :literal true
    :content (util/create-file-content 
               restore-lines))
  )

(defn create-source-transport
  [app-name instance-name source-transport-lines]
  (actions/remote-file
    (script-path-with-name app-name instance-name :source-transport)   
    :mode "700"
    :overwrite-changes true
    :literal true
    :content (util/create-file-content 
               source-transport-lines))
  (actions/symbolic-link 
    (script-path-with-name app-name instance-name :source-transport) 
    (str "/etc/cron.daily/20_" (cron-name instance-name :source-transport))
    :action :create)
  )


(defn install-backup-environment
  [& {:keys [app-name]}]
  (create-backup-source-user)
  (create-source-environment app-name)
  )

(defn install-backup-app-instance
  [& {:keys [app-name
             instance-name
             backup-lines 
             source-transport-lines 
             restore-lines]}]
  (create-source-backup app-name instance-name backup-lines)
  (create-source-transport app-name instance-name source-transport-lines)
  (create-source-restore app-name instance-name restore-lines)
  )