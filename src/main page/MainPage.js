import React from 'react'
import Sidebar from './Similar components/Sidebar'
import './Similar components/MainPage.css'
import { BrowserRouter as Router, Switch, Route } from 'react-router-dom'
import Adm from './Content/AdmContContent/Adm'
import HomeContent from './Content/HomeContent/HomeContent'
import Footer from './Similar components/Footer'
import HistoryContent from './Content/HistoryContent/HistoryContent'
import AdmMasini from './Content/AdmMasiniContent/AdmMasini'
import Reviews from './Content/ReviewsContent/Reviews'
import AdmStContent from './Content/AdmStatiiContent/AdmStContent'
import TripPlanner from './Content/Trip Planner/TripPlanner'
import Statistici from './Content/StatisticiContent/Statistici'
import AdaugareStatie from './Content/AdmStatiiContent/AdaugareStatie'
import AdaugarePlug from './Content/AdmStatiiContent/AdaugarePlug'
import AdaugareChPoint from './Content/AdmStatiiContent/AdaugareChPoint'

function MainPage() {

    const chPointObj = sessionStorage.getItem('nrChPoint') ? JSON.parse(sessionStorage.getItem('nrChPoint')) : {iValue : "" , nValue : ""} ;
    const plugObj = sessionStorage.getItem('nrPlugs') ? JSON.parse(sessionStorage.getItem('nrPlugs')) : {iValue : "" , nValue : ""} ;
    
    return (
        <div className="MainPage">
            <div className="ContentMainPage">
                <Sidebar />

                <Router>
                    <div className="divDreapta">
                        <Switch>
                            <Route path="/home/Adm-account" exact component={Adm} />
                            <Route path="/home" exact component={HomeContent} />
                            <Route path="/home/History" exact component={HistoryContent} />
                            <Route path="/home/Adm-cars" exact component={AdmMasini} />
                            <Route path="/home/Reviews" exact component={Reviews} />
                            <Route path="/home/Adm-station" exact component={AdmStContent} />
                            <Route path="/home/Trip-planner" exact component={TripPlanner} />
                            <Route path="/home/Stats" exact component={Statistici} />
                            <Route path="/home/Adm-station/add" exact component={AdaugareStatie} />
                            <Route path={`/home/Adm-station/add/point/${chPointObj.iValue}`} exact component={AdaugareChPoint} />
                            <Route path={`/home/Adm-station/add/point/plug/${plugObj.iValue}`} exact component={AdaugarePlug} />
                        </Switch>
                    </div>
                </Router>
            </div>
            <footer> <Footer /> </footer>
        </div>
    );
}

export default MainPage;