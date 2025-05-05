-- Sample recipe data for Cooking Master
-- Assuming user_id 1 exists in your users table

INSERT INTO recipe (title, description, cooking_time, difficulty, ingredients, instructions, user_id) VALUES
(
    'Classic Margherita Pizza',
    'A simple yet delicious traditional Italian pizza with fresh basil and mozzarella.',
    45,
    'Easy',
    '2 cups all-purpose flour
1 cup warm water
2 1/4 tsp active dry yeast
1 tsp salt
1 tbsp olive oil
2 cups fresh mozzarella
1/2 cup tomato sauce
Fresh basil leaves
Salt and pepper to taste',
    '1. Mix flour, yeast, and salt in a bowl
2. Add warm water and olive oil, knead for 10 minutes
3. Let dough rise for 1 hour
4. Roll out dough and add toppings
5. Bake at 450°F for 15-20 minutes',
    1
);

INSERT INTO recipe (title, description, cooking_time, difficulty, ingredients, instructions, user_id) VALUES
(
    'Chocolate Chip Cookies',
    'Soft and chewy chocolate chip cookies that are perfect for any occasion.',
    30,
    'Easy',
    '2 1/4 cups flour
1 cup butter, softened
3/4 cup sugar
3/4 cup brown sugar
2 eggs
1 tsp vanilla
1 tsp baking soda
2 cups chocolate chips',
    '1. Preheat oven to 375°F
2. Cream butter and sugars
3. Add eggs and vanilla
4. Mix in dry ingredients
5. Fold in chocolate chips
6. Bake for 10-12 minutes',
    1
);

INSERT INTO recipe (title, description, cooking_time, difficulty, ingredients, instructions, user_id) VALUES
(
    'Beef Stir Fry',
    'Quick and healthy beef stir fry with colorful vegetables.',
    25,
    'Medium',
    '1 lb beef strips
2 cups mixed vegetables
3 tbsp soy sauce
2 tbsp vegetable oil
2 cloves garlic
1 tbsp ginger
1 tbsp cornstarch
1/4 cup water',
    '1. Slice beef into thin strips
2. Heat oil in wok or large pan
3. Stir-fry beef until browned
4. Add vegetables and stir-fry
5. Mix sauce ingredients
6. Pour sauce and cook until thickened',
    1
);

INSERT INTO recipe (title, description, cooking_time, difficulty, ingredients, instructions, user_id) VALUES
(
    'Chicken Tikka Masala',
    'Creamy and flavorful Indian curry dish with tender chicken.',
    60,
    'Hard',
    '2 lbs chicken breast
1 cup yogurt
2 tbsp garam masala
4 cloves garlic
2 inch ginger
2 onions
2 cans tomato sauce
1 cup heavy cream
Fresh cilantro',
    '1. Marinate chicken in yogurt and spices
2. Grill or bake chicken
3. Make curry sauce with onions and spices
4. Add tomato sauce and cream
5. Simmer with chicken
6. Garnish with cilantro',
    1
);

INSERT INTO recipe (title, description, cooking_time, difficulty, ingredients, instructions, user_id) VALUES
(
    'Vegetable Lasagna',
    'Layers of pasta, vegetables, and cheese in a hearty Italian dish.',
    90,
    'Medium',
    '12 lasagna noodles
2 cups ricotta cheese
2 cups mozzarella
1 cup parmesan
3 cups marinara sauce
2 cups mixed vegetables
2 cloves garlic
Fresh basil',
    '1. Cook lasagna noodles
2. Prepare vegetable filling
3. Mix cheeses and herbs
4. Layer noodles, sauce, vegetables, and cheese
5. Bake at 375°F for 45 minutes
6. Let rest 15 minutes before serving',
    1
); 