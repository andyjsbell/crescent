import './App.css';
import {UserProfile} from "./UserProfile";
import { withAuthenticator, AmplifySignOut } from '@aws-amplify/ui-react'
import React from "react";
import awsExports from "./aws-exports";
import Amplify from "@aws-amplify/core";
import {listStates} from "graphql/queries";

Amplify.configure(awsExports);

function App() {
  return (
    <UserProfile/>
  );
}

export default withAuthenticator(App)
