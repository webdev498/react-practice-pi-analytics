// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import React from "react";
import type {SamplePatentApp} from "../services/Types";
import "../styles/components/_patentapplicationspanel.scss";
import {Cell, Grid} from "react-mdl";

type
PatentApplicationsPanelProps = {
  open: boolean,
  applications: Array < SamplePatentApp >
}

class SampleApplicantsPanel extends React.Component {
  props: PatentApplicationsPanelProps

  constructor(props: PatentApplicationsPanelProps) {
    super(props);
  }

  render() {

    const apps = this.props.applications
      ? this.props.applications.map(
        (app, index) =>
          <li key={index}>
            {app.appNum + (app.applicants ? " (Applicants: " + app.applicants.join(", ") + ")" : "") }
          </li>
      )
      : null;

    return (
      <Grid className={"patent-applications"}
            style={{height: (this.props.open ? this.props.applications.length * 42 : 0) + "px"}}>
        <Cell col="8"/>
        <Cell col="4">
          <ul>
            {apps}
          </ul>
        </Cell>
      </Grid>
    )
  }

}

export default SampleApplicantsPanel