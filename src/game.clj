(ns game
  (:require [datomic.client.api :as d]
            [db]))

(def game-schema [{:db/ident :game/players
                   :db/valueType :db.type/ref
                   :db/cardinality :db.cardinality/many
                   :db/doc "The players playing the game"}

                  {:db/ident :game/activeplayer
                   :db/valueType :db.type/ref
                   :db/cardinality :db.cardinality/one
                   :db/doc "The active player this turn"}

                  {:db/ident :game/turn
                   :db/valueType :db.type/long
                   :db/cardinality :db.cardinality/one
                   :db/doc "The current turn number"}

                  {:db/ident :game/step
                   :db/valueType :db.type/ref
                   :db/cardinality :db.cardinality/one
                   :db/doc "The current turn step"}

                 ;; game enums
                 ;; zones
                  {:db/ident :library}
                  {:db/ident :hand}
                  {:db/ident :battlefield}
                  {:db/ident :graveyard}
                  {:db/ident :stack}
                  {:db/ident :exile}

                 ;; phases/steps
                 ;; beginning phase
                  {:db/ident :untapstep}
                  {:db/ident :upkeepstep}
                  {:db/ident :drawstep}

                  {:db/ident :precombatmainphase}

                 ;; combat phase
                  {:db/ident :beginningofcombatstep}
                  {:db/ident :declareattackersstep}
                  {:db/ident :declareblockersstep}
                  {:db/ident :combatdamagefirststrikestep}
                  {:db/ident :combatdamagestep}
                  {:db/ident :endofcombatstep}

                  {:db/ident :postcombatmainphase}

                 ;; ending phase
                  {:db/ident :endstep}
                  {:db/ident :cleanupstep}])

(d/transact db/conn {:tx-data game-schema})