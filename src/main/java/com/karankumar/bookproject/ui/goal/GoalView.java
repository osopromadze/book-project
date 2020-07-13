/*
    The book project lets a user keep track of different books they've read, are currently reading or would like to read
    Copyright (C) 2020  Karan Kumar

    This program is free software: you can redistribute it and/or modify it under the terms of the
    GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
    warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with this program.
    If not, see <https://www.gnu.org/licenses/>.
 */

package com.karankumar.bookproject.ui.goal;

import com.karankumar.bookproject.backend.entity.Book;
import com.karankumar.bookproject.backend.entity.PredefinedShelf;
import com.karankumar.bookproject.backend.entity.ReadingGoal;
import com.karankumar.bookproject.backend.service.GoalService;
import com.karankumar.bookproject.backend.service.PredefinedShelfService;
import com.karankumar.bookproject.ui.MainView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Vaadin view that represents the reading goal page (the number of books or pages a user wants to have read by the end
 * of the year)
 */
@Route(value = "goal", layout = MainView.class)
@PageTitle("Goal | Book Project")
public class GoalView extends VerticalLayout {

    private static final Logger LOGGER = Logger.getLogger(GoalView.class.getName());
    private static final String BEHIND = "behind";
    private static final String AHEAD_OF = "ahead of";
    private static final int WEEKS_IN_YEAR = 52;

    private final Button setGoal;
    private final PredefinedShelfService predefinedShelfService;
    private final ProgressBar progressBar;

    private GoalService goalService;
    private H1 readingGoal;

    /**
     * Displays whether a user has met the goal, is ahead or is behind the goal
     */
    private H3 goalProgress;

    private Span progressPercentage;

    private Span booksToRead;

    public GoalView(GoalService goalService, PredefinedShelfService predefinedShelfService) {
        this.goalService = goalService;
        this.predefinedShelfService = predefinedShelfService;

        readingGoal = new H1();
        setGoal = new Button();
        goalProgress = new H3();
        booksToRead = new Span();
        progressBar = new ProgressBar();
        progressBar.setMaxWidth("500px");
        progressPercentage = new Span();
        progressPercentage.getElement().getStyle().set("font-style", "italic");

        configureSetGoal();
        getCurrentGoal();

        add(readingGoal, progressBar, progressPercentage, goalProgress, booksToRead, setGoal);
        setSizeFull();
        setAlignItems(Alignment.CENTER);
    }

    private void configureSetGoal() {
        setGoal.addClickListener(event -> {
            GoalForm goalForm = new GoalForm();
            add(goalForm);
            goalForm.openForm();

            goalForm.addListener(GoalForm.SaveEvent.class, this::saveGoal);
        });
    }

    private void getCurrentGoal() {
        List<ReadingGoal> goals = goalService.findAll();
        if (goals.size() == 0) {
            readingGoal.setText("Reading goal not set");
            setGoal.setText("Set goal");
        } else {
            updateReadingGoal(goals.get(0).getTarget(), goals.get(0).getGoalType());
        }
    }

    private void updateSetGoalText() {
        setGoal.setText("Update goal");
    }

    private void saveGoal(GoalForm.SaveEvent event) {
        if (event.getReadingGoal() != null) {
            LOGGER.log(Level.INFO, "Retrieved goal from form is not null");
            goalService.save(event.getReadingGoal());
            updateReadingGoal(event.getReadingGoal().getTarget(), event.getReadingGoal().getGoalType());
        } else {
            LOGGER.log(Level.SEVERE, "Retrieved goal from form is null");
        }
    }

    private void updateReadingGoal(int targetToRead, ReadingGoal.GoalType goalType) {
        PredefinedShelf readShelf = findReadShelf();
        if (readShelf == null || readShelf.getBooks() == null) {
            return;
        }

        LOGGER.log(Level.INFO, "Read shelf: " + readShelf);

        int booksReadThisYear = 0;
        int pagesReadThisYear = 0;
        boolean lookingForBooks = goalType.equals(ReadingGoal.GoalType.BOOKS);
        for (Book book : readShelf.getBooks()) {
            if (book != null) {
                if (lookingForBooks && book.getDateFinishedReading() != null &&
                        book.getDateFinishedReading().getYear() == LocalDate.now().getYear()) {
                    booksReadThisYear++;
                } else {
                    pagesReadThisYear += book.getNumberOfPages();
                }
            }
        }

        String haveRead = "You have read ";
        String outOf = " out of ";
        double progress;
        if (goalType.equals(ReadingGoal.GoalType.BOOKS)) {
            toggleBooksGoalInfo(true);
            readingGoal.setText(haveRead + booksReadThisYear + outOf + + targetToRead + " books");
            goalProgress.setText(calculateProgress(targetToRead, booksReadThisYear));
            progress = getProgress(targetToRead, booksReadThisYear);
        } else {
            toggleBooksGoalInfo(false);
            readingGoal.setText(haveRead + pagesReadThisYear + outOf + targetToRead + " pages");
            progress = getProgress(targetToRead, pagesReadThisYear);
        }
        progressBar.setValue(progress);
        progressPercentage.setText(String.format("%.2f%% completed", (progress * 100)));

        updateSetGoalText();
    }

    /**
     * Only books in the read shelf count towards the goal
     * @return the read shelf if it can be found, null otherwise.
     */
    private PredefinedShelf findReadShelf() {
        PredefinedShelf readShelf = null;

        for (PredefinedShelf p : predefinedShelfService.findAll()) {
            if (p.getShelfName().equals(PredefinedShelf.ShelfName.READ)) {
                readShelf = p;
                break;
            }
        }
        return readShelf;
    }

    /**
     * @param isOn if true, set the visibility of the book goal-specific text to true. Otherwise, set them to false
     */
    private void toggleBooksGoalInfo(boolean isOn) {
        goalProgress.setVisible(isOn);
        booksToRead.setVisible(isOn);
    }

    /**
     * Calculates the reading progress for the books goal only
     * @param booksToReadThisYear the number of books to read by the end of the year (the goal)
     * @param booksReadThisYear the number of books that have already been read by th end of the year
     * @return a String that displays whether the goal was met, or whether the user is ahead or behind schedule
     */
    private String calculateProgress(int booksToReadThisYear, int booksReadThisYear) {
        LOGGER.log(Level.INFO, "\nBooks to read this year: " + booksToReadThisYear);
        LOGGER.log(Level.INFO, "Books read this year: " + booksReadThisYear);

        String schedule = "";
        int booksStillToRead = booksToReadThisYear - booksReadThisYear;
        LOGGER.log(Level.INFO, "Books still to read: " + booksStillToRead);

        if (booksStillToRead <= 0) {
            schedule = "Congratulations for reaching your target!";
        } else {
            int weekOfYear = getWeekOfYear();
            int weeksLeftInYear = weeksLeftInYear(weekOfYear);
            double booksStillToReadAWeek = Math.ceil((double) booksStillToRead / weeksLeftInYear);
            booksToRead.setText("You need to read " + booksStillToReadAWeek +
                    " books a week on average to achieve your goal");

            int howManyBehindOrAhead = howFarAheadOrBehindSchedule(booksToReadThisYear, booksReadThisYear);
            schedule = String.format("You are %d books %s schedule", howManyBehindOrAhead,
                    behindOrAheadSchedule(booksReadThisYear, shouldHaveRead(booksToReadThisYear)));
        }
        return schedule;
    }

    /**
     * @param booksToReadThisYear the number of books to read by the end of the year (the goal)
     * @param booksReadThisYear the number of books read so far
     * @return the number of books that the user is ahead or behind schedule by
     */
    private int howFarAheadOrBehindSchedule(int booksToReadThisYear, int booksReadThisYear) {
        int shouldHaveRead = booksToReadFromStartOfYear(booksToReadThisYear) * getWeekOfYear();
        return Math.abs(shouldHaveRead - booksReadThisYear);
    }

    /**
     * @return the current week number of the year
     */
    private int getWeekOfYear() {
        LocalDate now = LocalDate.now();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        return now.get(weekFields.weekOfWeekBasedYear());
    }

    /**
     * @param weekOfYear the current week number of the year
     * @return the number of weeks left in the year from the current week
     */
    private static int weeksLeftInYear(int weekOfYear) {
        return (WEEKS_IN_YEAR - weekOfYear);
    }

    /**
     * @param booksToReadThisYear the number of books to read by the end of the year (the goal)
     * @return the number of books that should have been read a week (on average) from the start of the year
     */
    public static int booksToReadFromStartOfYear(int booksToReadThisYear) {
        return ((int) Math.ceil(booksToReadThisYear / WEEKS_IN_YEAR));
    }

    /**
     * @param booksToReadThisYear the number of books to read by the end of the year (the goal)
     * @return the number of books that the user should have ready by this point in the year in order to be on target
     */
    public int shouldHaveRead(int booksToReadThisYear) {
        return booksToReadFromStartOfYear(booksToReadThisYear) * getWeekOfYear();
    }

    /**
     * Note that this method assumes that the user is behind or ahead of schedule (and that they haven't met their goal)
     * @param booksReadThisYear the number of books read so far
     * @param shouldHaveRead the number of books that should have been ready by this point to be on schedule
     * @return a String denoting that the user is ahead or behind schedule
     */
    public static String behindOrAheadSchedule(int booksReadThisYear, int shouldHaveRead) {
        return (booksReadThisYear < shouldHaveRead) ? BEHIND : AHEAD_OF;
    }

    /**
     * Calculates a user's progress towards their reading goal
     * @param toRead the number of books to read by the end of the year (the goal)
     * @param read the number of books that the user has read so far
     * @return a fraction of the number of books to read over the books read. If greater than 1, 1.0 is returned
     */
    public static double getProgress(int toRead, int read) {
        double progress = (toRead == 0) ? 0 : ((double) read / toRead);
        return Math.min(progress, 1.0);
    }
}
