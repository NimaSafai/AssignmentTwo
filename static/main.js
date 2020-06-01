// If we are on a quiz page.
if (location.pathname.match('^/play/')) {
    // The ID is everything after the second slash.
    const quizId = location.pathname.split('/')[2];
    startQuiz(quizId);
}
// If we are on the "Create Quiz" page.
else if (location.pathname.match('^/create')) {
    enableAddQuestionButton();
}

// Download the JSON quiz data from the server and then show the quiz.
async function startQuiz(id) {
    const response = await fetch('/quiz/' + id);
    if (!response.ok) {
        const main = document.querySelector('main');
        const p = document.createElement('p');
        p.textContent = await response.text();
        main.append(p);
        return;
    }

    const quiz = await response.json();
    showQuiz(quiz);
}

// Show the quiz downloaded from the server.
function showQuiz(quiz) {
    const main = document.querySelector('main');
    main.className = 'quiz';
    let score = 0;

    // Create a <section> for each question, then show/hide each section based on the users's current position in the
    // quiz. Each section will contain its own question buttons, "Next Question" button, and so on, so we are not
    // reusing these between questions.
    const questionNodes = [];
    quiz.questions.forEach((question, questionIndex) => {
        // Create the <section> container node.
        const questionNode = document.createElement('section');
        questionNode.className = 'question';
        main.append(questionNode);

        // Also add it to the questionNodes list, because we need that later in our event handlers to hide/show
        // questions as the user proceeds in the quiz.
        questionNodes.push(questionNode);

        // Initially hide it unless it's the first one.
        if (questionIndex > 0) {
            questionNode.hidden = true;
        }

        // DOM methods are incredibly annoying and verbose, so let's use innerHTML to make this easier and clearer.
        questionNode.innerHTML =
            '<h1 class="quiz-title">Quiz: ' + quiz.title + (quiz.public ? '' : ' [private]') + '</h1>' +
            '<figure class="flag">' +
                '<img src="/flag?name=' + question.image_path + '">' +
            '</figure>' +
            '<h2 class="prompt">' + question.prompt + '</h2>'

        // This part requires some more complicated logic so let's accept defeat and use DOM methods for this. :(
        // What we are doing is creating the individual question buttons. For clarity, the result will look something
        // like this:
        //
        // <div class="options">
        //     <button type="button" class="incorrect">Alice</button>
        //     <button type="button" class="incorrect">Bob</button>
        //     <button type="button" class="correct">Carol</button>
        //     <button type="button" class="incorrect">Dave</button>
        // </div>
        const options = document.createElement('div');
        options.className = 'options';
        questionNode.append(options);
        const buttons = [1, 2, 3, 4].map(number => {
            const button = document.createElement('button');
            button.type = 'button';
            button.textContent = question['option_' + number];
            if (number === question.correct_option) {
                button.className = 'correct';
            }
            else {
                button.className = 'incorrect';
            }
            return button;
        });

        // For each of the option buttons for this question:
        buttons.forEach((button, buttonIndex) => {
            // Add it to the document.
            options.append(button);

            // Make it interactive.
            button.onclick = () => {
                // If this button is the correct option, increase the score by one.
                if (buttonIndex + 1 === question.correct_option) {
                    score += 1;
                }

                // Add a .guess class so we can highlight it, and also disable all the option buttons so the user cannot
                // change their guess.
                button.classList.add('guess');
                buttons.forEach(b => b.disabled = true);

                // Finally, enable the "Next Question" button.
                next.disabled = false;
            };
        });

        // Create the "Next Question" button for this question.
        const next = document.createElement('button');
        next.className = 'next';
        // Show a different label depending on whether this button belongs to the last question or not.
        if (questionIndex === quiz.questions.length - 1) {
            next.textContent = 'Show Results';
        }
        else {
            next.textContent = 'Next Question';
        }
        // It should start out disabled, because the user needs to select an option first.
        next.disabled = true;
        questionNode.appendChild(next);

        // Make it interactive.
        next.onclick = event => {
            // Hide all of the question <section> tags except the next one.
            questionNodes.forEach((node, nodeIndex) => {
                if (nodeIndex === questionIndex + 1) {
                    node.hidden = false;
                }
                else {
                    node.hidden = true;
                }
            });

            // If this is actually the last "Next Question" button, show the result instead.
            if (questionIndex === quiz.questions.length - 1) {
                main.innerHTML = '<p class="result big">Your score: ' + score + ' / ' + quiz.questions.length + '</p>';
            }
        };
    });
}

// Enable the "Add Question" button by having it clone the first question.
function enableAddQuestionButton() {
    const button = document.getElementById('add-question');
    button.onclick = event => {
        // Get all the questions.
        const fieldsets = Array.from(document.querySelectorAll('fieldset'));

        // Clone the first question, which we will use as a template.
        const newFieldset = document.querySelector('fieldset').cloneNode(true);

        // Clear any values in it.
        Array.from(newFieldset.querySelectorAll('input[type=text]')).forEach(input => input.value = '');
        Array.from(newFieldset.querySelectorAll('input[type=radio]')).forEach(input => input.checked = false);
        Array.from(newFieldset.querySelectorAll('select')).forEach(select => select.selectedIndex = -1);

        // Change any references to the number 1.
        const questionNumber = fieldsets.length + 1;
        newFieldset.innerHTML = newFieldset.innerHTML.replace(/question-1/g, 'question-' + questionNumber);
        newFieldset.innerHTML = newFieldset.innerHTML.replace(/Question #1/g, 'Question #' + questionNumber);

        // Add the new question immediately after the previous one.
        const lastFieldset = fieldsets[fieldsets.length - 1];
        lastFieldset.insertAdjacentElement('afterend', newFieldset);
    };
}