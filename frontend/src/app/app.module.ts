import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { RegisterPlayersComponent } from './register-players/register-players.component';
import { HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { GameTableComponent } from './game-table/game-table.component';
import { RaiseInputComponent } from './raise-input/raise-input.component';


@NgModule({
    declarations: [],
    imports: [
        AppComponent,
        FormsModule,
        BrowserModule,
        BrowserAnimationsModule,
        AppRoutingModule,
        HttpClientModule,
        RegisterPlayersComponent,
        GameTableComponent,
        RaiseInputComponent
    ],
    providers: [],
    bootstrap: [AppComponent]
})
export class AppModule { }
