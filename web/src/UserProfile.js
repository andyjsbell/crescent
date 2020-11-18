import React, {useEffect, useState} from "react";
import {Auth} from "aws-amplify";
import {Button, Col, Container, Row} from "shards-react";

export const UserProfile = () => {
    const [name, setName] = useState('')

    const load = async () => {
        try {
            const info = await Auth.currentUserInfo()
            setName(info.username)
        } catch (err) {
            console.error("failed to load user info:", err)
        }
    }

    const signout = async () => {
        console.log("signout called")
        await Auth.signOut()
        window.location.reload()
    }

    useEffect(() => {
        load()
            .then(_ => console.log("loaded user profile"))
            .catch(e => console.error("error loading user profile:", e))
    })

    return (
        <Container className="dr-example-container">
            <Row>
                <Col sm="12" md="4" lg="3">

                </Col>
                <Col sm="12" md="4" lg="6">
                    <h1>Crescent</h1>
                </Col>
                <Col sm="12" md="4" lg="3">
                    <div className="userpanel-container">
                        <span className="userpanel-item">{name}</span>
                        <Button onClick={() => signout()}>Signout</Button>
                    </div>
                </Col>
            </Row>
        </Container>
    )
}