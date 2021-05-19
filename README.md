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
            <game specific>
            <:key> {
                :public  <known to all>
                :private <known to player>
                :secret  <known to none>
            }
        }
    }
}
