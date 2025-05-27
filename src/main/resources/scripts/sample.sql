begin;
truncate pins restart identity;
insert into pins(location, category, tags) values
    (
        '50.01736, 19.88656'::point,
        'report',
        hstore(
            'description',
            'Since police station has been built here, I smell hydrogen '
            || 'sulfide every time I pass by.'
        )
    ),
    (
        '50.04845, 19.90754'::point,
        'report',
        hstore(
            'description',
            'I''ve noticed concerning structural damage beneath the bridge.'
        )
    ),
    (
        '50.01696, 19.93463'::point,
        'report.pothole',
        ''::hstore
    ),
    (
        '50.05001, 19.81835'::point,
        'report.fallen_tree',
        ''::hstore
    ),
    (
        '50.05730, 19.95797'::point,
        'lost',
        hstore(array[
            [
                'description',
                'I''ve lost a pair of orange polyester gloves somewhere near '
                || 'Grzegorzeckie roundabout.'
            ],
            ['email', 'magdalena@mapstories.io']
        ])
    ),
    (
        '50.06608, 19.95948'::point,
        'lost',
        hstore(array[
            [
                'description',
                'Couldn''t find my wallet right after I left the inner space '
                || 'of Mogilskie roundabout. Brown leather wallet, had around '
                || '45 euros plus change inside.'
            ],
            ['money', '45 eur'],
            ['reward', '10 eur'],
            ['email', 'leonard@mapstories.io']
        ])
    ),
    (
        '50.06326, 19.94352'::point,
        'found',
        hstore(array[
            [
                'description',
                'Found a pair of white knitted wool gloves on the pavement.'
            ],
            ['email', 'krakow.explorer@mapstories.io']
        ])
    ),
    (
        '50.06531, 19.92931'::point,
        'lost',
        hstore(array[
            [
                'description',
                'Lost my precious 5m tape-measure somewhere in Wislawa '
                || 'Szymborska Park'
            ],
            ['email', 'the.builder@mapstories.io']
        ])
    ),
    (
        '50.06511, 19.92838'::point,
        'found',
        hstore(array[
            [
                'description',
                'Found a 5m tape-measure under a bench.'
            ],
            ['email', 'jan@mapstories.io']
        ])
    ),
    (
        '50.03442, 19.99418'::point,
        'event',
        hstore(array[
            [
                'description',
                'I invite everyone to take part in a small amatour chess '
                || 'tournament on the Bagry beach!'
            ],
            ['when', '2025-07-09 14:00+2']
        ])
    ),
    (
        '50.06544, 19.94161'::point,
        'story',
        hstore(array[
            [
                'story',
                'Krakow Barbican was a fortified outpost connected to the city '
                || 'walls used in the 16 century.'
            ],
            ['url', 'https://en.wikipedia.org/wiki/Krak%C3%B3w_Barbican']
        ])
    ),
    (
        '50.04894, 19.93322'::point,
        'event',
        hstore(array[
            [
                'description',
                'I am going to play guitar here on Friday, come listen'
            ],
            ['when', '2025-06-02 18:00+2'],
            ['duration', '2 hours']
        ])
    ),
    (
        '50.05502, 19.92651'::point,
        'report.vandalism',
        hstore('description', 'Destroyed planter and disrespectful graffiti.')
    ),
    (
        '54.80456, 18.37163'::point,
        'story',
        hstore(
            'story',
            'Cool pixel art graffiti inspired by video games here!'
        )
    ),
    (
        '50.18100, 19.80726'::point,
        'report.abandoned_vehicle',
        hstore(array[
            ['model', 'Toyota Camry'],
            ['colour', 'white'],
            ['number_plate', 'K1 BOBER'],
            ['check_date', '2025-02-25'],
            ['description', 'Was left at the roadside']
        ])
    ),
    (
        '53.90601, 27.55742'::point,
        'story',
        hstore(
            'story',
            'There''s something charming about this street for me'
        )
    ),
    (
        '50.06839, 19.90592'::point,
        'event.rave',
        hstore(array[
            ['fee', 'free'],
            ['when', '2025-05-08 19:00+2'],
            ['age_limit', '16'],
            ['email', 'miasteczko@mapstories.io']
        ])
    );
commit;
