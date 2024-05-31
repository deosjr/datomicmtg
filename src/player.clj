(ns player
  (:require [datomic.client.api :as d]
            [db]))

(def player-schema [{:db/ident :player/name
                     :db/valueType :db.type/string
                     :db/unique :db.unique/identity
                     :db/cardinality :db.cardinality/one
                     :db/doc "The name of the player"}

                    {:db/ident :player/life
                     :db/valueType :db.type/long
                     :db/cardinality :db.cardinality/one
                     :db/doc "The life total of the player"}])

(d/transact db/conn {:tx-data player-schema})