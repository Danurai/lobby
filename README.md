## atom structure

- :user-hash {<name> <id>}
- gamelist {
    - <id> { :minp <int> :maxp <int> :has-ai <boolean> }
}
- :games {
    - <id> {
        :minp <int> :maxp <int> :has-ai <boolean>
        :game <text>
        :title <text>
        :private? <boolean>
        :owner <text> ( = user-hash key )
        :players #{set of user-hash names}
        :state {
            :status <:key>
            :response 
                (def <response> {
                    :id <key>
                    :msg <txt>
                    :options :key
                    :cb (fn [ state ?data uname] <state>)})
            <game specific>
            <BBTM 
                :activeskill 
                :srcplayer 
                :tgtplayer 
                :tgtcoach 
                :hlid
            >
            <:key> {
                :public  <known to all>
                :private <known to player>
                :secret  <known to none>
            }
        }
    }
}
