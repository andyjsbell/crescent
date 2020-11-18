import React, {useEffect, useState} from "react"
import {listStates} from "graphql/queries"
import {API, graphqlOperation} from "aws-amplify";

export const States = () => {

    const [states, setStates] = useState([])

    const fetchStates = async () => {
        console.log("fetchStates called")
        try {
            const stateData = await API.graphql(graphqlOperation(listStates))
            const states = stateData.data.listStates.items
            setStates(states)
        } catch (err) { console.error('error fetching states:', err) }
    }

    return (
        <div>
            {states.map(state =>
                {state.lfdId}
            )}
        </div>
    )
}