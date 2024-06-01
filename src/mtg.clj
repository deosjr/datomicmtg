(ns mtg
  (:require [datomic.client.api :as d]
            [db]
            [effect]))

(load "db")

(load "card")

; todo: load from parsing mtg api
(def first-cards  [{:card/name "Lava Spike"
                    :card/multiverseid 512313
                    :card/cost "R"
                    :card/type :sorcery
                    :card/rules "Lava Spike deals 3 damage to target player."}

                   {:card/name "Gray Ogre"
                    :card/multiverseid 204
                    :card/cost "2R"
                    :card/type :creature}

                   {:card/name "Hill Giant"
                    :card/multiverseid 205
                    :card/cost "3R"
                    :card/type :creature}

                   {:card/name "Mountain"
                    :card/multiverseid 291
                    :card/type :land
                    :card/rules "{T}: Add {R}."}

                   {:card/name "Grizzly Bears"
                    :card/multiverseid 155
                    :card/cost "1G"
                    :card/type :creature}

                   {:card/name "Forest"
                    :card/multiverseid 289
                    :card/type :land
                    :card/rules "{T}: Add {G}."}])

(d/transact db/conn {:tx-data first-cards})

(load "player")

(load "game")

(def players [{:player/name "Player1"
               :player/life 20}
              {:player/name "Player2"
               :player/life 20}])

(d/transact db/conn {:tx-data players})

(def all-players-q '[:find ?name ?life
                     :where [?e :player/name ?name] [?e :player/life ?life]])
(d/q all-players-q (d/db db/conn))

(def player1 [:player/name "Player1"])
(def player2 [:player/name "Player2"])

(def new-game [{:game/players [player1 player2]
                :game/activeplayer player1
                :game/turn 1
                :game/step :precombatmainphase}])

(d/transact db/conn {:tx-data new-game})

(load "effect")

(load "cardinstance")

(defn play-land [player cardname]
  [{:instance/card [:card/name cardname]
    :instance/zone :battlefield
    :instance/controller player
    :instance/owner player}])

(d/transact db/conn {:tx-data (play-land player1 "Mountain")})
(d/transact db/conn {:tx-data (play-land player1 "Mountain")})
(d/transact db/conn {:tx-data (play-land player1 "Mountain")})

(d/transact db/conn {:tx-data (play-land player2 "Forest")})
(d/transact db/conn {:tx-data (play-land player2 "Forest")})

; todo: nonlands use the stack

(defn cast-spell [player cardname]
  [{:instance/card [:card/name cardname]
    :instance/zone :battlefield
    :instance/controller player
    :instance/owner player}])

(d/transact db/conn {:tx-data (cast-spell player1 "Gray Ogre")})
(d/transact db/conn {:tx-data (cast-spell player1 "Hill Giant")})
(d/transact db/conn {:tx-data (cast-spell player2 "Grizzly Bears")})

; ?e is the identifier of the card instance
; ?tx is the transaction id in which location was last set
; ASSUMPTION: tx id is monotonically increasing (so no need for d/tx->t)
(def zone-q '[:find ?e ?tx
              :in $ ?name ?zone
              :where
              [?player :player/name ?name]
              [?e :instance/controller ?player]
              [?e :instance/zone ?zone ?tx]])
(def cards (sort-by second (d/q zone-q (d/db db/conn) "Player1" :battlefield)))

(defn cardinfo [eid]
  (d/pull (d/db db/conn) [{:instance/card [:card/name :card/type]}
                          [:instance/tapped :default false]] eid))

(defn zoneinfo [player zone selectors]
  (let [conn (d/db db/conn)
        cards (sort-by second (d/q zone-q conn player zone))]
    (map #(d/pull conn selectors %) (map first cards))))

(zoneinfo "Player1" :battlefield [{:instance/card [:card/name {:card/type [:db/ident]}]}])

; need to ask for instance/card/type/db/ident for this to work
(defn has-type? [type cardinfo]
  (some #(= type (get-in % [:db/ident])) (get-in cardinfo [:instance/card :card/type])))

(filter #(has-type? :land %)
        (zoneinfo "Player1" :battlefield [{:instance/card [:card/name {:card/type [:db/ident]}]}]))

(def play-lava-spike [{:instance/card [:card/name "Lava Spike"]
                       :instance/zone :stack
                       :effect/target [:player/name "Player2"]
                       :instance/controller [:player/name "Player1"]
                       :instance/owner [:player/name "Player1"]}])

(d/transact db/conn {:tx-data play-lava-spike})

; [eid] list to multiverseids
(defn mapmids [eids]
  (map #(d/pull (d/db db/conn) [{:instance/card [:card/multiverseid]}] (first %)) eids))

(def stack-q '[:find ?e
               :in $
               :where
               [?e :instance/zone :stack]])
(defn stack []
  (d/q stack-q (d/db db/conn)))

(def card-to-resolve (d/pull (d/db db/conn) [{:instance/card [:card/name :card/rules]}
                                             {:effect/target [:player/name]}]
                             (ffirst (stack))))
(def cardname (get-in card-to-resolve [:instance/card :card/name]))
(def targetname (get-in card-to-resolve [:effect/target :player/name]))

; todo: db/cas from stack?
(def resolve-card [:db/add (ffirst (stack)) :instance/zone :graveyard])

; merge effects and make a single transaction
(def result [resolve-card (effect/damage targetname 3)])
(d/transact db/conn {:tx-data result})

(d/q zone-q (d/db db/conn) "Player1" :battlefield)
(d/q zone-q (d/db db/conn) "Player1" :stack)
(d/q zone-q (d/db db/conn) "Player1" :graveyard)

(zoneinfo "Player1" :graveyard [{:instance/card [:card/name]}])

"mtg db loaded"