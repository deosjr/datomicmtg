(ns cardinstance
  (:require [datomic.client.api :as d]
            [db]))

(def instance-schema [{:db/ident :instance/card
                       :db/valueType :db.type/ref
                       :db/cardinality :db.cardinality/one
                       :db/doc "The card (type) being instantiated (token)"}

                      {:db/ident :instance/zone
                       :db/valueType :db.type/ref
                       :db/cardinality :db.cardinality/one
                       :db/doc "The zone in which the instance currently exists"}

                      {:db/ident :instance/controller
                       :db/valueType :db.type/ref
                       :db/cardinality :db.cardinality/one
                       :db/doc "The player that controls the card instance"}

                      {:db/ident :instance/owner
                       :db/valueType :db.type/ref
                       :db/cardinality :db.cardinality/one
                       :db/doc "The player who owns the card instance"}

                      {:db/ident :instance/tapped
                       :db/valueType :db.type/boolean
                       :db/cardinality :db.cardinality/one
                       :db/doc "Whether the card is tapped (true) or untapped (false/nonexistent)"}])

(d/transact db/conn {:tx-data instance-schema})