// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import React from "react";
import {DataTable, IconButton, TableHeader} from "react-mdl";
import "../styles/main.scss";
import {Agent, ServiceAddress} from "../services/Types";

type
AddressesListProps = {
  serviceAddress: Object,
  agents: Array,
  onSortServiceAddress: (serviceAddressId: string, agent: Agent) => void,
  onUnsortServiceAddress: (serviceAddressId: string) => void
}

class AddressesList extends React.Component {
  props: AddressesListProps;

  constructor(props: AddressesListProps) {
    super(props);
  }

  renderLawFirmId(agent: Agent): React.Element<any> {
    if (agent.lawFirm) {
      var href = "".concat("dataServices/firmInfo.jsp?id=", agent.lawFirm.lawFirmId, "#agents");
      return <span>{agent.lawFirm.lawFirmId}&nbsp;<a href={href} target="_blank"><IconButton accent name="open_in_new"/></a></span>
    }
    return <span style={{color: "#bbb"}}>Non law firm</span>
  }

  renderEntity(agent: Agent): React.Element<any> {
    if (agent.lawFirm) {
      return <a href="#" onClick={(event: Event) => {
        event.preventDefault();
        this.props.onSortServiceAddress(this.props.serviceAddress.serviceAddressId, agent);
      }}>{agent.lawFirm.name}</a>
    }
    return <span>{agent.nonLawFirm.name}</span>
  }

  renderAddressLine(value: ServiceAddress, testValue: String): React.Element<Layout> {
    var result = new String(value.address);
    if (testValue && testValue.length > 0) {
      testValue
        .replace(/[;:.,]/g, " ")
        .replace(/  /g, " ")
        .split(" ")
        .filter(item => value.address.indexOf(item) >= 0)
        .forEach(item => {
          result = result.split(item).join("<span class='highlighted'>" + item + "</span>");
        });
    }
    return (
      <div><span dangerouslySetInnerHTML={{__html: result}}/><a href="#"><IconButton name="cancel" accent onClick={() => {
        this.props.onUnsortServiceAddress(value.serviceAddressId);
      }}/></a></div>
    );
  }

  renderServiceAddressWithInclusions(agent: Agent, testValue: String): Array<React.Element> {
    return agent.serviceAddresses.map(value => this.renderAddressLine(value, testValue));
  }

  renderWebsite(agent: Agent): ?React.Element<any> {
    if (agent.lawFirm && agent.lawFirm.websiteUrl) {
      return <a href={agent.lawFirm.websiteUrl} target="_blank"><IconButton name="public" accent/></a>
    }
    return null
  }

  renderEntityId(agent: Agent): React.Element<any> {
    return agent.serviceAddresses
      .map(
        (address, index) => <div>{address.serviceAddressId + (index < agent.serviceAddresses.length - 1 ? "," : "")}</div>)
  };

  mapRows() {
    return this.props.agents.map((agent: Agent, index: number) => ({
      key: index,
      lawFirmId: this.renderLawFirmId(agent),
      entity: this.renderEntity(agent),
      serviceAddress: this.renderServiceAddressWithInclusions(agent, this.props.serviceAddress.address),
      website: this.renderWebsite(agent),
      serviceAddressId: this.renderEntityId(agent)
    }))
  };

  render() {

    return (
      <DataTable style={{width: "100%"}} rowKeyColumn="key" rows={this.mapRows()}>
        <TableHeader name="lawFirmId">Law Firm ID</TableHeader>
        <TableHeader name="entity">Firm</TableHeader>
        <TableHeader name="serviceAddress">Service Addresses</TableHeader>
        <TableHeader name="website">www</TableHeader>
        <TableHeader name="serviceAddressId">Entity ID</TableHeader>
      </DataTable>
    )
  }
}

export default AddressesList
