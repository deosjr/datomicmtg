(ns card
  (:require [datomic.client.api :as d]
            [db]))

(def card-schema [{:db/ident :card/name
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one
                   :db/unique :db.unique/value
                   :db/doc "The name of the card"}

                  {:db/ident :card/cost
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one
                   :db/doc "The mana cost of the card"}

                  {:db/ident :card/type
                   :db/valueType :db.type/ref
                   :db/cardinality :db.cardinality/many
                   :db/doc "The card type(s) of the card"}

                  {:db/ident :creature}
                  {:db/ident :land}
                  {:db/ident :artifact}
                  {:db/ident :enchantment}
                  {:db/ident :sorcery}
                  {:db/ident :instant}

                  {:db/ident :card/rules
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one
                   :db/doc "The rules of the card"}

                  ; https://docs.magicthegathering.io/#documentationgetting_started
                  {:db/ident :card/multiverseid
                   :db/valueType :db.type/long
                   :db/cardinality :db.cardinality/one
                   :db/unique :db.unique/identity
                   :db/doc "The multiverseid of the card on Wizard's Gatherer web page"}

                  ; creatures
                  {:db/ident :creature/power
                   :db/valueType :db.type/long
                   :db/cardinality :db.cardinality/one
                   :db/doc "The power of the creature"}
                  {:db/ident :creature/toughness
                   :db/valueType :db.type/long
                   :db/cardinality :db.cardinality/one
                   :db/doc "The power of the creature"}])

(d/transact db/conn {:tx-data card-schema})