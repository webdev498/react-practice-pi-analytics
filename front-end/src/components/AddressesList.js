// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import React from "react";
import {DataTable, IconButton, TableHeader} from "react-mdl";
import "../styles/main.scss";
import type {Agent, ServiceAddress} from "../services/Types";
import {OuterUrls} from "../services/Urls";
import AddressLine from "./AddressLine";

type
AddressesListProps = {
  serviceAddress: Object,
  agents: Array < Agent >,
  onSortServiceAddress: (serviceAddressId: string, agent: Agent) => void,
  onUnsortServiceAddress: (serviceAddressId: string) => void
}

class AddressesList extends React.Component {
  props: AddressesListProps
  state: Object

  constructor(props: AddressesListProps) {
    super(props)
    this.state = {selected: ""}
  }

  getSelectedClassName(agentIndex: number, addressIndex: number) {
    return this.state.selected === agentIndex + ":" + addressIndex ? "selected" : ""
  }

  select(agentIndex: number, addressIndex: number, disabled: boolean) {
    if (!disabled) {
      this.setState(prevState => ({
        selected: agentIndex + ":" + addressIndex,
      }))
    }
  }

  unselect(agentIndex: number, addressIndex: number) {
    if (this.state.selected === agentIndex + ":" + addressIndex) {
      this.setState(prevState => ({selected: ""}))
    }
  }

  renderLawFirmId(agent: Agent): React.Element<any> {
    if (agent.lawFirm) {
      const lawFirmId = agent.lawFirm ? agent.lawFirm.lawFirmId : 0
      const href = OuterUrls.firmInfo + lawFirmId + "#agents"
      const website = this.renderWebsite(agent)
      return (
        <span>
          {lawFirmId}&nbsp;
          <a href={href} target="_blank">
            <IconButton accent name="open_in_new"/>
          </a>{website}
        </span>
      )
    }
    return <span style={{color: "#bbb"}}>Non law firm</span>
  }

  renderFirmName(agent: Agent): React.Element<any> {
    if (agent.lawFirm) {

      const onClick = (event: Event) => {
        event.preventDefault()
        this.props.onSortServiceAddress(this.props.serviceAddress.serviceAddressId, agent)
      }

      return <span><a className="entity" href="#" onClick={onClick}>{agent.lawFirm.name}</a></span>
    }
    return <span className="entity">{agent.nonLawFirm ? agent.nonLawFirm.name : ""}</span>
  }

  renderAddressLine(agentIndex: number, index: number, value: ServiceAddress, testValue: string): React.Element<any> {
    let result = value.name
    if (value.address) {
      result = result + ", " + value.address
    }
    const fullText = "" + result;
    const upperCase = result.toUpperCase()

    //here we going to add highlight tags to indicate service address and law firm address/name intersections.
    if (testValue && testValue.length > 0 && value.address) {

      //clean address string. remove ; : . , ' "
      const cleaned = testValue.replace(/[;:.,'"]/g, " ").replace(/  /g, " ");

      cleaned
        .toUpperCase()
        .split(" ")
        .sort((a, b) => b.length - a.length)
        .filter(item => item.length > 1 && upperCase.indexOf(item) >= 0)
        .forEach(item => {
          //prepare lookup variants: unchanged, uppercase, lowercase, capitalized
          [
            cleaned.substr(cleaned.toUpperCase().indexOf(item), item.length),
            item,
            item.toLowerCase(),
            item.substr(0, 1) + item.substr(1).toLowerCase()
          ]
            .forEach(test => {
              //insert temporary non-alphanum tags for each occurrence of the test item to avoid possible
              //undesirable intersections with span tag name and class attribute.
              result = result.split(test).join("<==>" + test + "</==>")
            });
        });
      //insert real highlight tags.
      result = result.replace(/<==>/g, "<span class='highlighted'>").replace(/<\/==>/g, "</span>");
    }
    return (
      <div key={index} onMouseOver={() => this.select(agentIndex, index, agentIndex < 0)}
           onMouseLeave={() => this.unselect(agentIndex, index)}
           className={"address-line " + this.getSelectedClassName(agentIndex, index)}>
        <AddressLine
          parentClass={"address-line"}
          text={value.address || value.name ? result : ""}
          fullText={value.address || value.name ? fullText : ""}/>
      </div>
    );
  }

  renderServiceAddressWithInclusions(agentIndex: number, agent: Agent, testValue: string): Array<any> {
    return agent.serviceAddresses.map(
      (value, index) => this.renderAddressLine(agent.serviceAddresses.length > 1 ? agentIndex : -1,
                                               index, value, testValue))
  }

  renderWebsite(agent: Agent): ?React.Element<any> {
    if (agent.lawFirm && agent.lawFirm.websiteUrl) {
      let href = agent.lawFirm.websiteUrl;
      href = (agent.lawFirm.websiteUrl.indexOf("http") == -1 ? "http://" : "") + href;
      return (
        <a href={href} target="_blank">
          <IconButton name="public" accent/>
        </a>
      )
    }
    return null
  }

  renderEntityId(agentIndex: number, agent: Agent): Array<any> {
    return agent.serviceAddresses
      .map((address, index) => <div key={index}
                                    onMouseOver={() => this.select(agentIndex, index, agent.serviceAddresses.length < 2)}
                                    onMouseLeave={() => this.unselect(agentIndex, index)}
                                    className={
                                      "address-line " + this.getSelectedClassName(agentIndex, index)
                                    }>{address.serviceAddressId}</div>)
  }

  renderActions(agentIndex: number, agent: Agent): Array<any> {
    return agent.serviceAddresses.map(
      (address, index) => <div key={index}
                               onMouseOver={() => this.select(agentIndex, index, agent.serviceAddresses.length < 2)}
                               onMouseLeave={() => this.unselect(agentIndex, index)}
                               className={this.getSelectedClassName(agentIndex, index)}
                               style={{paddingLeft: "2px", marginBottom: "3px"}}>
        <a href="#">
          <IconButton name="cancel" accent onClick={() => {
            this.props.onUnsortServiceAddress(address.serviceAddressId)
          }}/>
        </a>
      </div>)
  }

  mapRows() {
    return this.props.agents.map((agent: Agent, index: number) => ({
      key: index,
      lawFirmId: this.renderLawFirmId(agent),
      entity: this.renderFirmName(agent),
      serviceAddress: this.renderServiceAddressWithInclusions(index, agent, this.props.serviceAddress.name + " "
                                                                            + this.props.serviceAddress.address),
      serviceAddressId: this.renderEntityId(index, agent),
      actions: this.renderActions(index, agent),
    }))
  }

  render() {
    return (
      <DataTable className="addresses-list" rowKeyColumn="key"
                 rows={this.mapRows()}>
        <TableHeader style={{width: "12%"}} name="lawFirmId">Law Firm ID</TableHeader>
        <TableHeader style={{width: "21%"}} name="entity">Firm Name</TableHeader>
        <TableHeader style={{paddingLeft: "26px", width: "54%"}} name="serviceAddress">Service Addresses</TableHeader>
        <TableHeader style={{paddingLeft: "26px", width: "7%"}} name="serviceAddressId">Entity ID</TableHeader>
        <TableHeader style={{width: "6%"}} name="actions">Re-sort</TableHeader>
      </DataTable>
    )
  }
}

export default AddressesList
